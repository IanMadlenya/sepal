package org.openforis.sepal.scene.retrieval

import org.apache.commons.io.FileUtils
import org.openforis.sepal.scene.DownloadRequest
import org.openforis.sepal.scene.ScenePublisher
import org.openforis.sepal.scene.SceneRequest
import org.openforis.sepal.scene.Status
import org.openforis.sepal.scene.retrieval.provider.DownloadRequestObservable
import org.openforis.sepal.scene.retrieval.provider.SceneRetrievalObservable
import org.openforis.sepal.util.FilePermissions

import static Status.PUBLISHED
import static Status.PUBLISHING

class SepalScenePublisher implements ScenePublisher {
    @Delegate
    @SuppressWarnings("GroovyUnusedDeclaration")
    private final SceneRetrievalObservable sceneRetrievalObservable = new SceneRetrievalObservable()

    @Delegate
    @SuppressWarnings("GroovyUnusedDeclaration")
    private final DownloadRequestObservable downloadRequestObservable = new DownloadRequestObservable()


    private final SceneRepository sceneRepository

    SepalScenePublisher(SceneRepository sceneRepository) {
        this.sceneRepository = sceneRepository
    }

    void publish(DownloadRequest downloadRequest) {
        notifyRequestStatusChange(downloadRequest, PUBLISHING)
        doPublish(
                sceneRepository.getDownloadRequestWorkingDirectory(downloadRequest),
                sceneRepository.getDownloadRequestHomeDirectory(downloadRequest)
        )
        notifyRequestStatusChange(downloadRequest, PUBLISHED)
    }

    void publish(SceneRequest request) {
        notifyListeners(request, PUBLISHING)
        doPublish(
                sceneRepository.getSceneWorkingDirectory(request),
                sceneRepository.getSceneHomeDirectory(request)
        )
        notifyListeners(request, PUBLISHED)
    }

    private void notifyRequestStatusChange(DownloadRequest request, Status status) {
        request.scenes.each { notifyListeners(it, status) }
        notifyDownloadRequestListeners(request, status)
    }

    private void doPublish(File src, File dest) {
        FilePermissions.readWritableRecursive(src)
        if (dest.exists()) {
            dest.deleteDir()
        }
        FileUtils.moveDirectory(src, dest)
    }


}