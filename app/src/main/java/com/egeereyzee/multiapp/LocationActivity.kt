package com.egeereyzee.multiapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Environment

class LocationActivity : AppCompatActivity() {
    private lateinit var textViewLatitude: TextView
    private lateinit var textViewLongitude: TextView
    private lateinit var textViewAltitude: TextView
    private lateinit var textViewCurrentTime: TextView
    private lateinit var buttonGetLocation: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationRequestCode = 100

    private var latitudeValue: Double = 0.0
    private var longitudeValue: Double = 0.0
    private var altitudeValue: Double = 0.0
    private var currentTimeValue: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        initializeViews()
        setupButton()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun initializeViews() {
        textViewLatitude = findViewById(R.id.textViewLatitude)
        textViewLongitude = findViewById(R.id.textViewLongitude)
        textViewAltitude = findViewById(R.id.textViewAltitude)
        textViewCurrentTime = findViewById(R.id.textViewCurrentTime)
        buttonGetLocation = findViewById(R.id.buttonGetLocation)
    }

    private fun setupButton() {
        buttonGetLocation.setOnClickListener {
            checkLocationPermission()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                locationRequestCode
            )
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    updateLocationData(location)
                    saveLocationToFile()
                }
            }
    }

    private fun updateLocationData(location: Location) {
        latitudeValue = location.latitude
        longitudeValue = location.longitude
        altitudeValue = location.altitude

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        currentTimeValue = dateFormat.format(Date())

        textViewLatitude.text = "Latitude: $latitudeValue"
        textViewLongitude.text = "Longitude: $longitudeValue"
        textViewAltitude.text = "Altitude: $altitudeValue"
        textViewCurrentTime.text = "Current Time: $currentTimeValue"
    }

    private fun saveLocationToFile() {
        val locationData = LocationData(
            latitude = latitudeValue,
            longitude = longitudeValue,
            altitude = altitudeValue,
            currentTime = currentTimeValue
        )

        val gson = Gson()
        val jsonString = gson.toJson(locationData)

        val fileName = "location_data.json"

        if (isExternalStorageWritable()) {
            val appSpecificExternalDir = getExternalFilesDir(null)
            val file = File(appSpecificExternalDir, fileName)

            try {
                val fileWriter = FileWriter(file, true)
                fileWriter.append(jsonString + "\n")
                fileWriter.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    private fun readLocationFile(): String {
        val fileName = "location_data.json"
        val appSpecificExternalDir = getExternalFilesDir(null)
        val file = File(appSpecificExternalDir, fileName)

        return if (file.exists()) {
            file.readText()
        } else {
            "File does not exist"
        }
    }

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val currentTime: String
    )

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            }
        }
    }
}