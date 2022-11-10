package pt.pedrocorreia.myapplication

import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    private val locationManager : LocationManager by lazy {
        getSystemService(LOCATION_SERVICE) as LocationManager
    }

    private var coarseLocationPermission = false
    private var fineLocationPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}