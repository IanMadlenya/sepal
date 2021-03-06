/**
 * @author Mino Togna
 */
var EventBus  = require( '../../../../event/event-bus' )
var Events    = require( '../../../../event/events' )
var MapLoader = require( '../../../../map-loader/map-loader' )


var Layer = function ( params ) {
    var $this        = this
    $this.properties = params
    $this.id         = params.id
    $this.timestamp  = new Date().getTime()
    // $this.opacity    = 1
    // $this.pendingUrls = []
    
    MapLoader.load( function ( google ) {
        $this.loading = false
        // var options = this.getImageMapTypeOptions( params )
        var options   = {
            
            getTileUrl: function ( tile, zoom ) {
                tile        = getNormalizedCoord( tile, zoom )
                var y       = tile.y
                y           = Math.pow( 2, zoom ) - y - 1
                var baseUrl = '/sandbox/geo-web-viz/layer/' + params.id
                if ( !$this.loading ) {
                    // console.log( 'Starting', $this.id )
                    EventBus.dispatch( Events.APPS.DATA_VIS.MAP_LAYER_TILES_LOADING )
                }
                $this.loading = true
                
                var url = [ baseUrl, zoom, tile.x, y ].join( '/' ) + '.png?' + $this.timestamp
                
                //test
                // $this.pendingUrls.push( url )
                // if ( $this.pendingUrls.length === 1 ) {
                //     console.log( 'Overlay busy ', $this.id )
                
                // $($this.imageMapType).trigger("overlay-busy");
                // }
                
                return url
            },
            
            tileSize: new google.maps.Size( 256, 256 )
        }
        
        
        $this.imageMapType = new google.maps.ImageMapType( options )
        $this.imageMapType.addListener( 'tilesloaded', function () {
            // console.log( 'tilesloaded', $this.id )
            $this.loading = false
        } )
        
        
        //test
        // Listen for our custom events
        // $( $this.imageMapType ).bind( "overlay-idle", function () {
        //     console.log( "overlay is idle", $this.id );
        // } )
        //
        // $( $this.imageMapType ).bind( "overlay-busy", function () {
        //     console.log( "overlay is busy", $this.id );
        // } )
        //
        // $this.imageMapType.baseGetTile = $this.imageMapType.getTile
        //
        // Override getTile so we may add event listeners to know when the images load
        // $this.imageMapType.getTile = function ( tileCoord, zoom, ownerDocument ) {
        //
        //     // Get the DOM node generated by the out-of-the-box ImageMapType
        //     var node = $this.imageMapType.baseGetTile( tileCoord, zoom, ownerDocument );
        //     // console.log(node , $this.id)
        //     // Listen for any images within the node to finish loading
        //     // $( "img", node ).one( "load", function () {
        //     // console.log( $( node ).find('img')  ,$this.id)
        //     // $( node ).on('load', 'img', function () {
        //     if($(node).find('img').length>0){
        //     node.getElementsByTagName("img")[0].onload = function() {
        //     // $( node ).load(function () {
        //
        //         // Remove the image from our list of pending urls
        //         var index = $.inArray( this.__src__, $this.pendingUrls )
        //         console.log( "BEFORE SPLICE", this.__src__, $this.pendingUrls )
        //         $this.pendingUrls.splice( index, 1 );
        //         console.log( "AFTER SPLICE", this.__src__, $this.pendingUrls )
        //
        //         // If the pending url list is empty, emit an event to
        //         // indicate that the tiles are finished loading
        //         if ( $this.pendingUrls.length === 0 ) {
        //             console.log( 'Overlay idle ', $this.id )
        //             // $($this.imageMapType).trigger("overlay-idle");
        //         }
        //     } }//)
        //
        //     return node
        // }
        //
        // $this.imageMapType.releaseTileBck = $this.imageMapType.releaseTile
        // $this.imageMapType.releaseTile = function (tileDiv  ) {
        //     console.log( "BEFORE RELEASE TILE" , tileDiv)
        //     var x = $this.imageMapType.releaseTileBck(tileDiv)
        //     console.log( "RELEASE TILE", x )
        //     return x
        // }
        
    } )
}

var getNormalizedCoord = function ( coord, zoom ) {
    var y = coord.y
    var x = coord.x
    
    // tile range in one direction range is dependent on zoom level
    // 0 = 1 tile, 1 = 2 tiles, 2 = 4 tiles, 3 = 8 tiles, etc
    var tileRange = 1 << zoom
    
    // don't repeat across y-axis (vertically)
    if ( y < 0 || y >= tileRange ) {
        return null
    }
    
    // repeat across x-axis
    if ( x < 0 || x >= tileRange ) {
        x = (x % tileRange + tileRange) % tileRange
    }
    
    return { x: x, y: y }
}


var newInstance = function ( params ) {
    return new Layer( params )
}

module.exports = {
    newInstance: newInstance
}