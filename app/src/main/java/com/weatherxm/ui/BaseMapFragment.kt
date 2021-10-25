package com.weatherxm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.scalebar.scalebar
import com.weatherxm.databinding.FragmentMapBinding
import com.weatherxm.util.ResourcesHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

open class BaseMapFragment : Fragment(), KoinComponent {

    protected lateinit var binding: FragmentMapBinding
    protected lateinit var polygonManager: PolygonAnnotationManager
    protected val resourcesHelper: ResourcesHelper by inject()

    protected var mapListener: MapListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mapView.getMapboxMap()
            .loadStyleUri("mapbox://styles/exmachina/ckrxjh01a5e7317plznjeicao") {
                Timber.d("MapBox is ready and style loaded")

                binding.mapView.gestures.rotateEnabled = false
                binding.mapView.scalebar.enabled = false

                val annotationPlugin = binding.mapView.annotations
                polygonManager = annotationPlugin.createPolygonAnnotationManager(binding.mapView)

                mapListener?.onMapReady()
            }
    }

    interface MapListener {
        fun onMapReady()
    }
}
