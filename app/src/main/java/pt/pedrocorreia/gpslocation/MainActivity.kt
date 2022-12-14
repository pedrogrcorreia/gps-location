package pt.pedrocorreia.gpslocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import pt.pedrocorreia.gpslocation.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    /*private val locationManager: LocationManager by lazy {
        getSystemService(LOCATION_SERVICE) as LocationManager
    }*/

    companion object {
        private val ISEC = LatLng(40.1925, -8.4115)
        private val DEIS = LatLng(40.1925, -8.4128)
        private var currentLatLng = DEIS
    }

    private val locationProvider: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private var coarseLocationPermission = false
    private var fineLocationPermission = false
    private var backgroundLocationPermission = false
    private var locationEnabled = false

    private val nullString : String? = null

    private var currentLocation = Location(nullString)
        set(value) {
            field = value
            binding.tvLat.text = String.format("%10.5f", value.latitude)
            binding.tvLon.text = String.format("%10.5f", value.longitude)
            val previousLatLng = currentLatLng
            currentLatLng = LatLng(value.latitude, value.longitude)
            if(previousLatLng != currentLatLng)
                updateMap()
        }

    private var currentMap : GoogleMap? = null

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        verifyPermissions()

        (supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment)?.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        map.isMyLocationEnabled = true
        map.mapType = GoogleMap.MAP_TYPE_HYBRID
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true
        val cp = CameraPosition.Builder().target(ISEC).zoom(17f)
            .bearing(0f).tilt(0f).build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cp))
        map.addCircle(
            CircleOptions().center(ISEC).radius(150.0)
            .fillColor(Color.argb(128, 128, 128, 128))
            .strokeColor(Color.rgb(128, 0, 0)).strokeWidth(4f))
        val mo = MarkerOptions().position(ISEC).title("ISEC-IPC")
            .snippet("Instituto Superior de Engenharia de Coimbra")
        val isec = map.addMarker(mo)
        isec?.showInfoWindow()
        map.addMarker(MarkerOptions().position(DEIS).title("DEIS-ISEC"))
        currentMap = map
    }

    private fun updateMap(){
        val cp = CameraPosition.Builder().target(currentLatLng).zoom(17f)
            .bearing(0f).tilt(0f).build()
        currentMap?.apply {
            clear()
            animateCamera(CameraUpdateFactory.newCameraPosition(cp))
            addCircle(
                CircleOptions().center(currentLatLng).radius(150.0)
                    .fillColor(Color.argb(128, 128, 128, 128))
                    .strokeColor(Color.rgb(128, 0, 0)).strokeWidth(4f))
        }

    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun verifyPermissions() {
        coarseLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        fineLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else
            backgroundLocationPermission = coarseLocationPermission || fineLocationPermission

        if (!coarseLocationPermission && !fineLocationPermission) {
            basicPermissionsAuthorization.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else
            verifyBackgroundPermission()
    }

    private val basicPermissionsAuthorization = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        coarseLocationPermission = results[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        fineLocationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        startLocationUpdates()
        verifyBackgroundPermission()
    }

    private fun verifyBackgroundPermission() {
        if (!(coarseLocationPermission || fineLocationPermission))
            return

        if (!backgroundLocationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            ) {
                val dlg = AlertDialog.Builder(this)
                    .setTitle("Background Location")
                    .setMessage(
                        "This application needs your permission to use location while in the background.\n" +
                                "Please choose the correct option in the following screen" +
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                                    " (\"${packageManager.backgroundPermissionOptionLabel}\")."
                                else
                                    "."
                    )
                    .setPositiveButton("Ok") { _, _ ->
                        backgroundPermissionAuthorization.launch(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                    }
                    .create()
                dlg.show()
            }
        }
    }

    private val backgroundPermissionAuthorization = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        backgroundLocationPermission = result
        Toast.makeText(this,"Background location enabled: $result",Toast.LENGTH_LONG).show()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (locationEnabled || !(coarseLocationPermission || fineLocationPermission))
            return

        currentLocation = Location(nullString).apply{
            latitude = 40.1925
            longitude = -8.4128
        }

        locationProvider.lastLocation
            .addOnSuccessListener { location ->
                if (location == null) return@addOnSuccessListener
                currentLocation = location
            }

        val locationRequest =
            LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, 2000)
                //.setMinUpdateDistanceMeters(100f)
                //.setMinUpdateIntervalMillis(1000)
                //.setMaxUpdateDelayMillis(5000)
                //.setPriority(PRIORITY_HIGH_ACCURACY)
                //.setIntervalMillis(2000)
                //.setMaxUpdates(10)
                .build()
        locationProvider.requestLocationUpdates(locationRequest,locationCallback, Looper.getMainLooper())

        locationEnabled = true
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            result.locations.forEach { location ->
                currentLocation = location
            }
        }
    }

    private fun stopLocationUpdates() {
        if (!locationEnabled)
            return
        locationProvider.removeLocationUpdates(locationCallback)
        locationEnabled = false
    }
}