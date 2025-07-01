package com.steptracker.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.steptracker.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WalkTrackingService : Service() {
    
    private val binder = WalkTrackingBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    
    private val _routePoints = MutableStateFlow<List<Location>>(emptyList())
    val routePoints: StateFlow<List<Location>> = _routePoints.asStateFlow()
    
    private val _totalDistance = MutableStateFlow(0.0)
    val totalDistance: StateFlow<Double> = _totalDistance.asStateFlow()
    
    private val _maxElevation = MutableStateFlow(0.0)
    val maxElevation: StateFlow<Double> = _maxElevation.asStateFlow()
    
    private val _elevationGain = MutableStateFlow(0.0)
    val elevationGain: StateFlow<Double> = _elevationGain.asStateFlow()
    
    private var lastLocation: Location? = null
    private var lastElevation: Double = 0.0
    
    companion object {
        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "walk_tracking_channel"
        private const val CHANNEL_NAME = "Walk Tracking"
        private const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
        private const val LOCATION_FASTEST_INTERVAL = 3000L // 3 seconds
    }
    
    inner class WalkTrackingBinder : Binder() {
        fun getService(): WalkTrackingService = this@WalkTrackingService
    }
    
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
    
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    _currentLocation.value = location
                    updateRouteData(location)
                }
            }
        }
    }
    
    private fun updateRouteData(newLocation: Location) {
        // Add to route points
        _routePoints.value = _routePoints.value + newLocation
        
        // Calculate distance
        lastLocation?.let { last ->
            val distance = last.distanceTo(newLocation)
            _totalDistance.value = _totalDistance.value + distance
        }
        
        // Update elevation data
        val elevation = newLocation.altitude
        if (elevation > _maxElevation.value) {
            _maxElevation.value = elevation
        }
        
        if (lastElevation > 0 && elevation > lastElevation) {
            _elevationGain.value = _elevationGain.value + (elevation - lastElevation)
        }
        
        lastLocation = newLocation
        lastElevation = elevation
    }
    
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            .build()
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Handle permission not granted
        }
    }
    
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows walk tracking progress"
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Walk Tracking Active")
            .setContentText("Tracking your walk...")
            .setSmallIcon(R.drawable.ic_walk)
            .setOngoing(true)
            .build()
    }
    
    fun resetWalkData() {
        _routePoints.value = emptyList()
        _totalDistance.value = 0.0
        _maxElevation.value = 0.0
        _elevationGain.value = 0.0
        lastLocation = null
        lastElevation = 0.0
    }
    
    fun getRoutePolyline(): String {
        // In a real implementation, you would encode the route points to a polyline
        // For now, return empty string
        return ""
    }
} 