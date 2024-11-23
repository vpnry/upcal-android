package upcal.cd.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.GeolocationPermissions;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.WindowManager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.os.IBinder;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class MainActivity extends Activity {

    private WebView mWebView;
    private boolean isLocationPermissionDenied = false;
    private upCalGPS gpsService;
    private boolean isBound = false;

    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            upCalGPS.LocalBinder binder = (upCalGPS.LocalBinder) service;
            gpsService = binder.getService();
            gpsService.setMainActivity(MainActivity.this);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Keep screen on

        setContentView(R.layout.activity_main);
        mWebView = findViewById(R.id.activity_main_webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setGeolocationEnabled(true);

        // Enable localStorage
        webSettings.setDomStorageEnabled(true);

        // Zoom controls
        webSettings.setDisplayZoomControls(false);
        webSettings.setBuiltInZoomControls(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        }

        // Enable Cache Mode
        webSettings.setAllowFileAccess(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ONLY);

        mWebView.setWebViewClient(new MyWebViewClient());

        // Use setWebChromeClient for JS alert
        mWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        // Load Local Resource
        mWebView.loadUrl("file:///android_asset/upcal_offline.html");

        mWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        // Bind to the location service
        Intent intent = new Intent(this, upCalGPS.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        checkAndRequestPermissions();
    }

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void getLocationByGPS() {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                runOnUiThread(MainActivity.this::showEnableGpsDialog);
            } else {
                if (isBound && gpsService != null) {
                    gpsService.startLocationUpdates();
                }
            }
        }
    }

    private void showEnableGpsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("GPS is not enabled")
                .setMessage("Do you want to turn on GPS?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void checkAndRequestPermissions() {
        // Check if location permissions are granted
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    public void updateLocation(double latitude, double longitude, float accuracy) {
        // Pass location data to WebView's JavaScript function
        mWebView.post(() -> mWebView.evaluateJavascript(
                "handleSuccess({ coords: { latitude: " + latitude + ", longitude: " + longitude + ", accuracy: "
                        + accuracy + " } });",
                null));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted
                enableLocationFeatures();
                isLocationPermissionDenied = false;
            } else {
                // Permission denied
                Toast.makeText(this, "Location permission is required to get location updates.", Toast.LENGTH_LONG)
                        .show();
                isLocationPermissionDenied = true;
            }
        }
    }

    private void enableLocationFeatures() {
        // Enable location in WebView
        mWebView.getSettings().setGeolocationEnabled(true);
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false); // Grant permission
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
