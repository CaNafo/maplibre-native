package org.maplibre.android.testapp.activity.turf

import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import org.maplibre.geojson.*
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshot
import org.maplibre.android.snapshotter.MapSnapshotter
import org.maplibre.android.style.expressions.Expression.within
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property.NONE
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.testapp.databinding.ActivityMapsnapshotterWithinExpressionBinding
import org.maplibre.android.testapp.styles.TestStyles.getPredefinedStyleWithFallback

/**
 * An Activity that showcases the use of MapSnapshotter with 'within' expression
 */
class MapSnapshotterWithinExpression : AppCompatActivity() {
    private lateinit var binding: ActivityMapsnapshotterWithinExpressionBinding
    private lateinit var maplibreMap: MapLibreMap
    private lateinit var snapshotter: MapSnapshotter
    private var snapshotInProgress = false

    private val cameraListener = object : MapView.OnCameraDidChangeListener {
        override fun onCameraDidChange(animated: Boolean) {
            if (!snapshotInProgress) {
                snapshotInProgress = true
                snapshotter.setCameraPosition(maplibreMap.cameraPosition)
                snapshotter.start(object : MapSnapshotter.SnapshotReadyCallback {
                    override fun onSnapshotReady(snapshot: MapSnapshot) {
                        binding.imageView.setImageBitmap(snapshot.bitmap)
                        snapshotInProgress = false
                    }
                })
            }
        }
    }

    private val snapshotterObserver = object : MapSnapshotter.Observer {
        override fun onStyleImageMissing(imageName: String) {
        }

        override fun onDidFinishLoadingStyle() {
            // Show only POI labels inside geometry using within expression
            (snapshotter.getLayer("poi-label") as SymbolLayer).setFilter(
                within(
                    bufferLineStringGeometry()
                )
            )
            // Hide other types of labels to highlight POI labels
            (snapshotter.getLayer("road-label") as SymbolLayer).setProperties(visibility(NONE))
            (snapshotter.getLayer("transit-label") as SymbolLayer).setProperties(visibility(NONE))
            (snapshotter.getLayer("road-number-shield") as SymbolLayer).setProperties(visibility(NONE))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsnapshotterWithinExpressionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { map ->
            maplibreMap = map

            // Setup camera position above Georgetown
            maplibreMap.cameraPosition = CameraPosition.Builder().target(LatLng(38.90628988399711, -77.06574689337494)).zoom(15.5).build()

            // Wait for the map to become idle before manipulating the style and camera of the map
            binding.mapView.addOnDidBecomeIdleListener(object : MapView.OnDidBecomeIdleListener {
                override fun onDidBecomeIdle() {
                    maplibreMap.easeCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder().zoom(16.0).target(LatLng(38.905156245642814, -77.06535338052844)).bearing(80.68015859462369).tilt(55.0).build()
                        ),
                        1000
                    )
                    binding.mapView.removeOnDidBecomeIdleListener(this)
                }
            })
            // Load mapbox streets and add lines and circles
            setupStyle()
        }
    }

    private fun setupStyle() {
        // Assume the route is represented by an array of coordinates.
        val coordinates = listOf<Point>(
            Point.fromLngLat(-77.06866264343262, 38.90506061276737),
            Point.fromLngLat(-77.06283688545227, 38.905194197410545),
            Point.fromLngLat(-77.06285834312439, 38.906429843444094),
            Point.fromLngLat(-77.0630407333374, 38.90680554236621)
        )

        // Setup style with additional layers,
        // using streets as a base style
        maplibreMap.setStyle(
            Style.Builder().fromUri(getPredefinedStyleWithFallback("Streets"))
        ) {
            binding.mapView.addOnCameraDidChangeListener(cameraListener)
        }

        val options = MapSnapshotter.Options(binding.imageView.measuredWidth / 2, binding.imageView.measuredHeight / 2)
            .withCameraPosition(maplibreMap.cameraPosition)
            .withPixelRatio(2.0f).withStyleBuilder(
                Style.Builder().fromUri(getPredefinedStyleWithFallback("Streets")).withSources(
                    GeoJsonSource(
                        POINT_ID,
                        LineString.fromLngLats(coordinates)
                    ),
                    GeoJsonSource(
                        FILL_ID,
                        FeatureCollection.fromFeature(
                            Feature.fromGeometry(bufferLineStringGeometry())
                        ),
                        GeoJsonOptions().withBuffer(0).withTolerance(0.0f)
                    )
                ).withLayerBelow(
                    LineLayer(LINE_ID, POINT_ID).withProperties(
                        lineWidth(7.5f),
                        lineColor(Color.LTGRAY)
                    ),
                    "poi-label"
                ).withLayerBelow(
                    CircleLayer(POINT_ID, POINT_ID).withProperties(
                        circleRadius(7.5f),
                        circleColor(Color.DKGRAY),
                        circleOpacity(0.75f)
                    ),
                    "poi-label"
                ).withLayerBelow(
                    FillLayer(FILL_ID, FILL_ID).withProperties(
                        fillOpacity(0.12f),
                        fillColor(Color.YELLOW)
                    ),
                    LINE_ID
                )
            )
        snapshotter = MapSnapshotter(this, options)
        snapshotter.setObserver(snapshotterObserver)
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        binding.mapView.onSaveInstanceState(outState)
    }
        private fun bufferLineStringGeometry(): Polygon {
        // TODO replace static data by Turf#Buffer: mapbox-java/issues/987
        // # --8<-- [start:fromJson]
        return FeatureCollection.fromJson(
            """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {},
                  "geometry": {
                    "type": "Polygon",
                    "coordinates": [
                      [
                        [
                          -77.06867337226866,
                          38.90467655551809
                        ],
                        [
                          -77.06233263015747,
                          38.90479344272695
                        ],
                        [
                          -77.06234335899353,
                          38.906463238984344
                        ],
                        [
                          -77.06290125846863,
                          38.907206285691615
                        ],
                        [
                          -77.06364154815674,
                          38.90684728656818
                        ],
                        [
                          -77.06326603889465,
                          38.90637140121084
                        ],
                        [
                          -77.06321239471436,
                          38.905561553883246
                        ],
                        [
                          -77.0691454410553,
                          38.905436318935635
                        ],
                        [
                          -77.06912398338318,
                          38.90466820642439
                        ],
                        [
                          -77.06867337226866,
                          38.90467655551809
                        ]
                      ]
                    ]
                  }
                }
              ]
            }
            """.trimIndent()
        ).features()!![0].geometry() as Polygon
        // # --8<-- [end:fromJson]
    }

    companion object {
        const val POINT_ID = "point"
        const val FILL_ID = "fill"
        const val LINE_ID = "line"
    }
}
