package com.lifekit.app;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int LOCATION_REQ = 1001;
    private static final int NOTIFICATION_REQ = 1002;
    private static final String CHANNEL_ID = "lifekit_general";
    private static final String APP_URL = "https://hiakdhs323-cmd.github.io/LifeKit/";

    private WebView webView;
    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setGeolocationEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.setWebViewClient(new WebViewClient() {
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    Toast.makeText(MainActivity.this, "LifeKit 연결에 문제가 있어요. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (hasLocation()) {
                    callback.invoke(origin, true, false);
                    return;
                }
                pendingGeoOrigin = origin;
                pendingGeoCallback = callback;
                requestLocationPermission();
            }
            @Override public void onPermissionRequest(PermissionRequest request) { request.deny(); }
        });

        if (hasLocation()) requestNotificationPermission();
        else requestLocationPermission();

        webView.loadUrl(APP_URL);
    }

    private boolean hasLocation() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        if (!hasLocation()) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_REQ);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQ);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "LifeKit 알림",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("LifeKit의 생활 알림");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 70, 45, 70});
            channel.setLightColor(Color.rgb(49, 130, 246));
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    public void showLifeKitNotification(String title, String text) {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        Notification.BigTextStyle style = new Notification.BigTextStyle()
                .bigText(text == null ? "새로운 알림이 있어요." : text);

        builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title == null ? "LifeKit" : title)
                .setContentText(text == null ? "새로운 알림이 있어요." : text)
                .setStyle(style)
                .setAutoCancel(true)
                .setColor(Color.rgb(49, 130, 246))
                .setVibrate(new long[]{0, 70, 45, 70});

        getSystemService(NotificationManager.class)
                .notify((int) (System.currentTimeMillis() & 0x7fffffff), builder.build());
    }

    public void vibrateShort() {
        Vibrator vibrator = Build.VERSION.SDK_INT >= 31
                ? ((VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator()
                : (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(45);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQ) {
            boolean granted = hasLocation();
            if (pendingGeoCallback != null) {
                pendingGeoCallback.invoke(pendingGeoOrigin, granted, false);
                pendingGeoCallback = null;
                pendingGeoOrigin = null;
            }
            if (!granted) {
                Toast.makeText(this, "GPS 기능을 사용하려면 위치 권한이 필요합니다.", Toast.LENGTH_LONG).show();
            }
            requestNotificationPermission();
        } else if (requestCode == NOTIFICATION_REQ
                && Build.VERSION.SDK_INT >= 33
                && !hasNotificationPermission()) {
            Toast.makeText(this, "알림 권한을 허용하면 LifeKit 알림을 받을 수 있어요.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
