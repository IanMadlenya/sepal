package org.openforis.sepal.component.datasearch.adapter

import groovy.sql.Sql
import org.openforis.sepal.component.datasearch.api.DataSet
import org.openforis.sepal.component.datasearch.api.SceneMetaData
import org.openforis.sepal.component.datasearch.api.SceneMetaDataRepository
import org.openforis.sepal.component.datasearch.api.SceneQuery
import org.openforis.sepal.sql.SqlConnectionManager

import java.sql.Connection

class JdbcSceneMetaDataRepository implements SceneMetaDataRepository {
    private final SqlConnectionManager connectionManager

    JdbcSceneMetaDataRepository(SqlConnectionManager connectionManager) {
        this.connectionManager = connectionManager
    }

    void updateAll(Collection<SceneMetaData> scenes) {
        def sql = new Sql(connectionManager.dataSource)
        sql.withTransaction {
            scenes.each {
                update(it, sql)
            }
        }
    }

    private void update(SceneMetaData scene, Sql sql) {
        def params = scene.with {
            [sensorId, sceneAreaId, acquisitionDate, cloudCover, sunAzimuth, sunElevation,
             browseUrl.toString(), updateTime, dataSet.metaDataSource, id]
        }

        def rowsUpdated = sql.executeUpdate('''
                UPDATE scene_meta_data
                SET   sensor_id = ?,
                      scene_area_id = ?,
                      acquisition_date = ?,
                      cloud_cover = ?,
                      sun_azimuth = ?,
                      sun_elevation = ?,
                      browse_url = ?,
                      update_time = ?,
                      meta_data_source = ?
                WHERE id = ?''', params)
        if (!rowsUpdated)
            sql.executeInsert('''
                    INSERT INTO scene_meta_data(
                        sensor_id, scene_area_id, acquisition_date, cloud_cover, sun_azimuth, sun_elevation, 
                        browse_url, update_time, meta_data_source, id)
                    VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)''', params)
    }

    List<SceneMetaData> findScenesInSceneArea(SceneQuery query) {
        return sql.rows('''
                SELECT id, meta_data_source, sensor_id, scene_area_id, acquisition_date, cloud_cover, 
                       sun_azimuth, sun_elevation, browse_url, update_time
                FROM scene_meta_data
                WHERE scene_area_id = ?
                AND acquisition_date >= ? AND acquisition_date <= ? AND acquisition_date <= ?''',
                [query.sceneAreaId, query.fromDate, query.toDate, latestAcquisitionDate()])
                .collect { toSceneMetaData(it) }
    }

    void eachScene(SceneQuery query, double targetDayOfYearWeight, Closure<Boolean> callback) {
        def q = """
                SELECT
                    (1 - $targetDayOfYearWeight) * cloud_cover / 100 + $targetDayOfYearWeight *
                    LEAST(
                        ABS(DAYOFYEAR(acquisition_date) - $query.targetDayOfYear),
                        365 - ABS(DAYOFYEAR(acquisition_date) - $query.targetDayOfYear)) / 182 as sort_weight,
                    LEAST(
                        ABS(DAYOFYEAR(acquisition_date) - $query.targetDayOfYear),
                        365 - ABS(DAYOFYEAR(acquisition_date) - $query.targetDayOfYear)) days_from_target_date,
                    id, meta_data_source, sensor_id, scene_area_id, acquisition_date, cloud_cover, 
                    sun_azimuth, sun_elevation, browse_url, update_time
                FROM scene_meta_data
                WHERE scene_area_id  = ?
                AND sensor_id in (${placeholders(query.sensorIds)})
                AND acquisition_date >= ? AND acquisition_date <= ? AND acquisition_date <= ?
                ORDER BY sort_weight, cloud_cover, days_from_target_date""" as String


        sql.withTransaction { Connection conn ->
            def ps = conn.prepareStatement(q)
            def i = 0
            ps.setString(++i, query.sceneAreaId)
            query.sensorIds.each {
                ps.setString(++i, it)
            }
            ps.setDate(++i, new java.sql.Date(query.fromDate.time))
            ps.setDate(++i, new java.sql.Date(query.toDate.time))
            ps.setDate(++i, new java.sql.Date(latestAcquisitionDate().time))
            def rs = ps.executeQuery()
            while (rs.next()) {
                def scene = new SceneMetaData(
                        id: rs.getString('id'),
                        dataSet: query.dataSet,
                        sceneAreaId: rs.getString('scene_area_id'),
                        sensorId: rs.getString('sensor_id'),
                        acquisitionDate: new Date(rs.getTimestamp('acquisition_date').time as long),
                        cloudCover: rs.getDouble('cloud_cover'),
                        sunAzimuth: rs.getDouble('sun_azimuth'),
                        sunElevation: rs.getDouble('sun_elevation'),
                        browseUrl: URI.create(rs.getString('browse_url')),
                        updateTime: new Date(rs.getTimestamp('update_time').time as long)
                )
                if (!callback.call(scene))
                    break
            }
            rs.close()
            ps.close()
        }
    }

    Map<String, Date> lastUpdateBySensor(DataSet dataSet) {
        def lastUpdates = [:]
        def sql = new Sql(connectionManager.dataSource)
        sql.withTransaction {
            sql.rows('''
                    SELECT sensor_id, MAX(update_time) last_update
                    FROM scene_meta_data
                    WHERE meta_data_source = ?
                    GROUP BY sensor_id''', [dataSet.metaDataSource]).each {
                lastUpdates[it.sensor_id] = new Date(it.last_update.time as long)
            }
        }
        return lastUpdates
    }

    private SceneMetaData toSceneMetaData(Map row) {
        new SceneMetaData(
                id: row.id,
                dataSet: DataSet.fromMetaDataSource(row.meta_data_source),
                sceneAreaId: row.scene_area_id,
                sensorId: row.sensor_id,
                acquisitionDate: new Date(row.acquisition_date.time as long),
                cloudCover: row.cloud_cover,
                sunAzimuth: row.sun_azimuth,
                sunElevation: row.sun_elevation,
                browseUrl: URI.create(row.browse_url),
                updateTime: new Date(row.update_time.time as long)
        )
    }

    private placeholders(Collection c) {
        (['?'] * c.size()).join(', ')
    }

    private getSql() {
        connectionManager.sql
    }

    /**
     * To make sure the actual data provider have the imagery, don't include results with images newer than this date.
     */
    private Date latestAcquisitionDate() {
        new Date() - 10
    }
}