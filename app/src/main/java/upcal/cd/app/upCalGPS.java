package upcal.cd.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class upCalGPS extends Service {

    private final IBinder binder = new LocalBinder();
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private MainActivity mainActivity;

    // Constants for thresholds
    private static final long MIN_TIME_BW_UPDATES = 1000; // 1 second
    private static final float MIN_DISTANCE_CHANGE = 1; // 1 meter

    public class LocalBinder extends Binder {
        upCalGPS getService() {
            return upCalGPS.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setMainActivity(MainActivity activity) {
        this.mainActivity = activity;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationCallback();
        // startForegroundService();
    }

    private void startForegroundService() {
        String CHANNEL_ID = "upCalGPSChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "upCal GPS Service",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("upCal GPS Service")
                .setContentText("Getting location updates")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!locationResult.getLocations().isEmpty()) {
                    android.location.Location location = locationResult.getLastLocation();
                    if (mainActivity != null) {
                        assert location != null;
                        if (location.getAccuracy() <= 20) { // 20 meters accuracy threshold
                            mainActivity.updateLocation(location.getLatitude(), location.getLongitude(),
                                    location.getAccuracy());
                            stopLocationUpdates(); // Stop updates after getting a good location
                        }
                    }
                }
            }
        };
    }

    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // Start foreground service when starting location updates
        startForegroundService();

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(MIN_TIME_BW_UPDATES) // 1-second interval
                .setMinUpdateIntervalMillis(500) // Fastest update every 0.5 seconds
                .setMinUpdateDistanceMeters(MIN_DISTANCE_CHANGE) // 1 meter
                .build();

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);

        // Retrieve cached last known location for immediate update
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && mainActivity != null) {
                mainActivity.updateLocation(location.getLatitude(), location.getLongitude(), location.getAccuracy());
            }
        });
    }

    public void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);

        // Stop foreground service when stopping location updates
        stopForeground(true); // Stops the notification
        stopSelf(); // Stops the service itself
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }
}
