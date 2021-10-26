package upcal.cd.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.GeolocationPermissions;

public class MainActivity extends Activity {

    private WebView mWebView;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            
            // Ref: https://developer.android.com/reference/android/webkit/GeolocationPermissions
            // https://turbomanage.wordpress.com/2012/04/23/how-to-enable-geolocation-in-a-webview-android/
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });


        // LOCAL RESOURCE
        mWebView.loadUrl("file:///android_asset/upcal_offline.html");
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
