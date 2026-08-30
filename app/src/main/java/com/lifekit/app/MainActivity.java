package com.lifekit.app;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int LOCATION_REQ = 1001;
    private static final int NOTIFICATION_REQ = 1002;
    private static final String CHANNEL_ID = "lifekit_general";
    private WebView webView;
    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();
        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setGeolocationEnabled(true);
        s.setAllowFileAccess(false); s.setAllowContentAccess(false); s.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                boolean fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                boolean coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                if (fine || coarse) callback.invoke(origin, true, false);
                else { pendingGeoOrigin = origin; pendingGeoCallback = callback; requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQ); }
            }
            @Override public void onPermissionRequest(PermissionRequest request) { request.deny(); }
        });
        if (!hasLocation()) requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQ);
        requestNotificationPermission();
        webView.loadUrl("https://raw.githubusercontent.com/hiakdhs323-cmd/LifeKit/main/index.html");
    }

    private boolean hasLocation() { return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED; }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQ);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL_ID, "LifeKit 알림", NotificationManager.IMPORTANCE_DEFAULT);
            c.setDescription("LifeKit의 생활 알림"); c.enableVibration(true); c.setVibrationPattern(new long[]{0,80,50,80});
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    public void showLifeKitNotification(String title, String text) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        b.setSmallIcon(R.drawable.ic_launcher).setContentTitle(title == null ? "LifeKit" : title).setContentText(text == null ? "새로운 알림이 있어요." : text).setAutoCancel(true).setVibrate(new long[]{0,80,50,80});
        getSystemService(NotificationManager.class).notify((int)(System.currentTimeMillis() & 0x7fffffff), b.build());
    }

    public void vibrateShort() {
        Vibrator v = Build.VERSION.SDK_INT >= 31 ? ((VibratorManager)getSystemService(VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator() : (Vibrator)getSystemService(VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)); else v.vibrate(45);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQ) {
            boolean granted = hasLocation();
            if (pendingGeoCallback != null) { pendingGeoCallback.invoke(pendingGeoOrigin, granted, false); pendingGeoCallback = null; pendingGeoOrigin = null; }
            if (!granted) Toast.makeText(this, "GPS 기능을 사용하려면 위치 권한이 필요합니다.", Toast.LENGTH_LONG).show();
        } else if (requestCode == NOTIFICATION_REQ && (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) && Build.VERSION.SDK_INT >= 33) {
            Toast.makeText(this, "알림 권한을 허용하면 LifeKit 알림을 받을 수 있어요.", Toast.LENGTH_LONG).show();
        }
    }
    @Override public void onBackPressed() { if (webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { if (webView != null) webView.destroy(); super.onDestroy(); }
}
