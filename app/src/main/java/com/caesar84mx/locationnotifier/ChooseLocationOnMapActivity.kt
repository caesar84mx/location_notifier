package com.caesar84mx.locationnotifier

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.View
import android.widget.SeekBar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_choose_location_on_map.*

const val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
const val MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2

class ChooseLocationOnMapActivity : FragmentActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener, SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private var mMap: GoogleMap? = null
    private var locationManager: LocationManager? = null

    private var circle: Circle? = null
    private var marker: Marker? = null

    private var submittableLocation: LatLng? = null
    private var submittableRadius: Double = 100.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_location_on_map)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sbSetRadius.progress = submittableRadius.toInt()

        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap

        Utility.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION, this)
        Utility.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION, this)

        val latitude = if (Utility.networkProviderAvailable(locationManager)) {
            locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).latitude
        } else {
            locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER).latitude
        }

        val longitude = if (Utility.networkProviderAvailable(locationManager)) {
            locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).longitude
        } else {
            locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER).longitude
        }

        val position = LatLng(latitude, longitude)

        mMap?.addCircle(
            CircleOptions()
                .center(position)
                .radius(2.0)
                .strokeColor(Color.BLUE)
                .fillColor(Color.BLUE))

        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16.0f))

        mMap?.setOnMapClickListener(this)
    }

    override fun onMapClick(position: LatLng?) {
        if (circle != null) {
            circle?.remove()
        }

        if (marker != null) {
            marker?.remove()
        }

        submittableLocation = position
        marker = mMap?.addMarker(MarkerOptions().position(position!!))
        circle = mMap?.addCircle(
            CircleOptions()
                .center(position)
                .radius(sbSetRadius.progress.toDouble())
                .strokeColor(Color.GREEN)
                .fillColor(0x127CD400)
                .strokeWidth(2f)
        )

        if (llRadiusSetterAndSubmit.visibility == View.GONE) {
            llRadiusSetterAndSubmit.visibility = View.VISIBLE
            sbSetRadius.setOnSeekBarChangeListener(this)
            btnSubmitLocation.setOnClickListener(this)
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        circle?.radius = progress.toDouble()
        submittableRadius = progress.toDouble()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onClick(v: View?) {
        val intent = Intent()
        intent.putExtra("location", submittableLocation)
        intent.putExtra("radius", submittableRadius)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
