package com.example.attendify

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.attendify.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.integration.android.IntentIntegrator
import com.vmadalin.easypermissions.EasyPermissions
import java.util.*


class MainActivity : AppCompatActivity() {

    private var address: String? = ""
    private lateinit var binding: ActivityMainBinding
    private val qrCodeScanner = IntentIntegrator(this)
    private var locationPermissionGranted: Boolean = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        geocoder = Geocoder(this, Locale.getDefault())

        lifecycleScope.launchWhenStarted {
            getLocation()
        }

        binding.scan.setOnClickListener {
            if(mapPermission()) {
                qrCodeScanner.initiateScan()
            } else {
                requestsLocationPermission()
            }
        }
    }

    private fun mapPermission(): Boolean {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        }
        else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {

                MaterialAlertDialogBuilder(this)
                    .setTitle("Permission Needed")
                    .setMessage("We need your Location access to upload event location")
                    .setPositiveButton("OK") { _, _ ->
                        requestsLocationPermission()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            else {
                requestsLocationPermission()
            }
        }
        return locationPermissionGranted
    }

    private fun requestsLocationPermission()  =
        EasyPermissions.requestPermissions(
            this,
            "We need your Location access to upload your attendance",
            1,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle the result of the QR code scan
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val spreadsheetUrl = result.contents

            insertDataIntoSpreadsheet(spreadsheetUrl)
        }
    }

    private fun insertDataIntoSpreadsheet(spreadsheetUrl: String) {

        val name = binding.username.text.toString().trim()
        val rollNo = binding.rollNo.text.toString().trim()

        Toast.makeText(this, "$name, $rollNo, $address",Toast.LENGTH_LONG).show()

        val stringRequest: StringRequest = object : StringRequest(Method.POST, spreadsheetUrl,
            Response.Listener { response ->
                Toast.makeText(this@MainActivity, response, Toast.LENGTH_LONG).show()
            },
            Response.ErrorListener { error ->
                Toast.makeText(this@MainActivity, error.toString(), Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): Map<String, String> {

                return mapOf(
                    "action" to "addItem",
                    "name" to name,
                    "rollNo" to rollNo,
                    "location" to "location"
                )
            }
        }

        val socketTimeOut = 50000 // u can change this .. here it is 50 seconds

        val retryPolicy: RetryPolicy =
            DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        stringRequest.retryPolicy = retryPolicy

        val queue = Volley.newRequestQueue(this)

        queue.add(stringRequest)
    }

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                val latitude = location.latitude
                val longitude = location.longitude

                address = getAddressFromLocation(latitude,longitude)
            }
        }
    }

    private fun getLocation() {
        if (mapPermission()) {
            if (isLocationEnabled()) {
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
                fusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location != null) {
                        address = getAddressFromLocation(location.latitude, location.longitude)
                    }
                }
            } else {
                Toast.makeText(this, "Please turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        var address: String? = ""
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    address = addresses[0]?.getAddressLine(0).toString()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return address
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

}