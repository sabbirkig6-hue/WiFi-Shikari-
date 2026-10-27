
package com.sabbir.wifishikari;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.net.InetAddress;
import java.util.List;

public class MainActivity extends Activity {

    private LinearLayout content;
    private WifiManager wifiManager;
    private final int bg = Color.rgb(8, 16, 35);
    private final int card = Color.rgb(20, 33, 59);
    private final int blue = Color.rgb(45, 135, 255);
    private final int white = Color.WHITE;
    private final int muted = Color.rgb(190, 202, 222);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(18, 22, 18, 28);
        scroll.addView(content);

        root.addView(scroll, new LinearLayout.LayoutParams(
                -1, 0, 1));

        setContentView(root);
        home();
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(0, 5, 0, 8);
        return t;
    }

    private void heading(String value) {
        TextView t = text(value, 25, white);
        t.setTypeface(null, Typeface.BOLD);
        content.addView(t);
    }

    private void paragraph(String value) {
        content.addView(text(value, 15, muted));
    }

    private Button button(String value, Runnable action) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(white);
        b.setAllCaps(false);
        b.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(blue));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                -1, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 6, 0, 8);
        content.addView(b, p);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void clear() {
        content.removeAllViews();
    }

    private void home() {
        clear();
        heading("📶 WiFi Shikari");
        paragraph("আপনার Wi-Fi, আপনার নিয়ন্ত্রণ");
        paragraph("নেটওয়ার্ক পরীক্ষা করুন, সমস্যা বুঝুন এবং সংযোগের মান জানুন।");

        button("📡 Wi-Fi Radar", this::radar);
        button("🩺 Smart Wi-Fi Doctor", this::doctor);
        button("🚀 Internet Speed Test", this::speedTest);
        button("📱 Device Finder", this::devices);
        button("⚙️ Wi-Fi Settings খুলুন", () ->
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        button("🔐 Wi-Fi নিরাপত্তা পরামর্শ", this::security);
    }

    private void radar() {
        clear();
        heading("📡 Wi-Fi Radar");
        paragraph("আশপাশের নেটওয়ার্ক স্ক্যান করতে Location অনুমতি ও ফোনের Location চালু থাকতে পারে।");

        button("স্ক্যান করুন", () -> {
            if (Build.VERSION.SDK_INT >= 23 &&
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
                return;
            }

            if (wifiManager == null || !wifiManager.isWifiEnabled()) {
                paragraph("Wi-Fi বন্ধ আছে। Wi-Fi Settings থেকে চালু করুন।");
                return;
            }

            boolean started = wifiManager.startScan();
            showScanResults();

            if (!started) {
                paragraph("Android স্ক্যান সীমিত করতে পারে। দেখানো ফলাফল পুরোনো হতে পারে।");
            }
        });

        button("← হোম", this::home);
    }

    private void showScanResults() {
        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            paragraph("Location অনুমতি প্রয়োজন।");
            return;
        }

        try {
            List<ScanResult> list = wifiManager.getScanResults();

            if (list == null || list.isEmpty()) {
                paragraph("এখনো কোনো ফলাফল নেই। কিছুক্ষণ পরে আবার চেষ্টা করুন।");
                return;
            }

            paragraph("পাওয়া নেটওয়ার্ক: " + list.size());

            for (ScanResult s : list) {
                String name = s.SSID == null || s.SSID.isEmpty()
                        ? "(নাম লুকানো)" : s.SSID;

                String strength;
                if (s.level >= -55) strength = "শক্তিশালী";
                else if (s.level >= -70) strength = "মাঝারি";
                else strength = "দুর্বল";

                content.addView(text(
                    "📶 " + name +
                    "\nসিগন্যাল: " + s.level + " dBm (" + strength + ")" +
                    "\nফ্রিকোয়েন্সি: " + s.frequency + " MHz" +
                    "\nনিরাপত্তা: " + s.capabilities,
                    15, white));
            }
        } catch (SecurityException e) {
            paragraph("অনুমতি না থাকায় স্ক্যান করা যায়নি।");
        }
    }

    private void doctor() {
        clear();
        heading("🩺 Smart Wi-Fi Doctor");
        paragraph("ফোনের নেটওয়ার্ক সংযোগ পরীক্ষা করা হচ্ছে...");

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        Network network = cm == null ? null : cm.getActiveNetwork();
        NetworkCapabilities caps = network == null || cm == null
                ? null : cm.getNetworkCapabilities(network);

        boolean connected = caps != null &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        boolean wifi = caps != null &&
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);

        if (!connected) {
            paragraph("⚠️ Internet সংযোগ শনাক্ত হয়নি। Wi-Fi চালু আছে কি না দেখুন, রাউটার পরীক্ষা করুন এবং প্রয়োজনে ইন্টারনেট সেবা প্রদানকারীর সঙ্গে যোগাযোগ করুন।");
        } else if (wifi) {
            paragraph("✅ ফোন Wi-Fi নেটওয়ার্কে সংযুক্ত।");

            if (caps.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                paragraph("✅ Android ইন্টারনেট সংযোগ যাচাই করেছে।");
            } else {
                paragraph("⚠️ Wi-Fi সংযুক্ত, কিন্তু ইন্টারনেট পুরোপুরি যাচাই হয়নি। রাউটার বা ISP-তে সমস্যা থাকতে পারে।");
            }
        } else {
            paragraph("📶 ইন্টারনেট চলছে, তবে বর্তমানে Wi-Fi নয়—অন্য সংযোগ ব্যবহৃত হচ্ছে।");
        }

        paragraph("নোট: এটি প্রাথমিক পরীক্ষা। রাউটারের সব ত্রুটি ফোন থেকে শনাক্ত করা সম্ভব নয়।");
        button("আবার পরীক্ষা করুন", this::doctor);
        button("← হোম", this::home);
    }

    private void speedTest() {
        clear();
        heading("🚀 Internet Speed Test");
        paragraph("এই সংস্করণে সংযোগের অবস্থা দেখা যাবে। প্রকৃত Mbps মাপতে নির্ভরযোগ্য পরীক্ষার সার্ভারে ডেটা পাঠানো ও গ্রহণ করতে হবে।");
        button("Speed Test ওয়েবসাইট খুলুন", () ->
                startActivity(new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("https://speed.cloudflare.com/"))));
        button("← হোম", this::home);
    }

    private void devices() {
        clear();
        heading("📱 Device Finder");
        paragraph("শুধু আশপাশের Wi-Fi স্ক্যান করে আপনার রাউটারে কতটি ডিভাইস যুক্ত আছে জানা যায় না।");
        paragraph("নিজের নেটওয়ার্কের ডিভাইসের সংখ্যা দেখতে রাউটারের অনুমোদিত Connected Devices পৃষ্ঠা বা নির্মাতার অ্যাপ ব্যবহার করুন।");
        paragraph("অনুমতি ছাড়া অন্যের নেটওয়ার্ক স্ক্যান করবেন না।");
        button("Wi-Fi Settings খুলুন", () ->
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        button("← হোম", this::home);
    }

    private void security() {
        clear();
        heading("🔐 Wi-Fi নিরাপত্তা");
        paragraph("• সম্ভব হলে WPA2-AES বা WPA3 ব্যবহার করুন।");
        paragraph("• রাউটারের অ্যাডমিন পাসওয়ার্ড শক্তিশালী রাখুন।");
        paragraph("• অচেনা ডিভাইস দেখলে নিজের রাউটারের তালিকা যাচাই করুন।");
        paragraph("• Wi-Fi পাসওয়ার্ড অচেনা কারও সঙ্গে শেয়ার করবেন না।");
        button("Wi-Fi Settings খুলুন", () ->
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        button("← হোম", this::home);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showScanResults();
        } else if (requestCode == 101) {
            paragraph("Radar ব্যবহারের জন্য Location অনুমতি প্রয়োজন।");
        }
    }
    }
