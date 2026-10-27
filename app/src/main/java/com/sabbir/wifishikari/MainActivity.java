package com.sabbir.wifishikari;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;

import java.util.List;

public class MainActivity extends Activity {

    private WifiManager wifiManager;
    private TextView results;
    private Button scanButton;
    private BroadcastReceiver receiver;
    private boolean receiverRegistered = false;

    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    100
                );
            } else {
                scanWifi();
            }
        } else {
            scanWifi();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 30, 24, 24);
        layout.setBackgroundColor(Color.rgb(12, 22, 42));

        TextView title = new TextView(this);
        title.setText("📶 WiFi Shikari");
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 20);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("আশেপাশের Wi-Fi নেটওয়ার্ক খুঁজুন");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 20);
        layout.addView(subtitle);

        scanButton = new Button(this);
        scanButton.setText("🔍 Wi-Fi স্ক্যান করুন");
        layout.addView(scanButton);

        ScrollView scrollView = new ScrollView(this);
        results = new TextView(this);
        results.setTextColor(Color.WHITE);
        results.setTextSize(16);
        results.setPadding(8, 20, 8, 20);
        results.setText("স্ক্যান করতে বোতামে চাপ দিন।\n"
                + "Wi-Fi চালু রাখুন এবং অনুমতি দিন।");

        scrollView.addView(results);
        layout.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(layout);

        scanButton.setOnClickListener(v -> requestPermissionsIfNeeded());

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                showNetworks();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (receiver != null && !receiverRegistered) {
            IntentFilter filter = new IntentFilter(
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(receiver, filter);
            }

            receiverRegistered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (receiverRegistered) {
            try {
                unregisterReceiver(receiver);
            } catch (Exception ignored) {
            }
            receiverRegistered = false;
        }
    }

    private void scanWifi() {
        if (wifiManager == null) {
            results.setText("এই ফোনে Wi-Fi সেবা পাওয়া যায়নি।");
            return;
        }

        if (!wifiManager.isWifiEnabled()) {
            results.setText("আগে ফোনের Wi-Fi চালু করুন, তারপর আবার স্ক্যান করুন।");
            return;
        }

        if (Build.VERSION.SDK_INT >= 23) {
            LocationManager lm = (LocationManager)
                    getSystemService(Context.LOCATION_SERVICE);

            if (lm != null
                    && !lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    && !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                results.setText("Wi-Fi স্ক্যানের জন্য ফোনের Location চালু করুন।");
                return;
            }
        }

        results.setText("স্ক্যান হচ্ছে... একটু অপেক্ষা করুন।");
        boolean started = wifiManager.startScan();

        if (!started) {
            showNetworks();
        }
    }

    private void showNetworks() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            results.setText("Wi-Fi স্ক্যানের জন্য Location অনুমতি দিন।");
            return;
        }

        List<ScanResult> networks = wifiManager.getScanResults();

        if (networks == null || networks.isEmpty()) {
            results.setText("কোনো নেটওয়ার্ক পাওয়া যায়নি। Wi-Fi ও Location চালু করে আবার চেষ্টা করুন।");
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append("পাওয়া নেটওয়ার্ক: ")
                .append(networks.size()).append("\n\n");

        for (ScanResult network : networks) {
            String name = network.SSID;

            if (name == null || name.trim().isEmpty()) {
                name = "(নাম লুকানো নেটওয়ার্ক)";
            }

            text.append("📶 ").append(name).append("\n");
            text.append("সিগন্যাল: ")
                    .append(network.level).append(" dBm\n");
            text.append("নিরাপত্তা: ")
                    .append(network.capabilities).append("\n");
            text.append("-------------------------\n");
        }

        results.setText(text.toString());
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanWifi();
            } else {
                results.setText("Wi-Fi স্ক্যান করতে Location অনুমতি প্রয়োজন।");
            }
        }
    }
}
