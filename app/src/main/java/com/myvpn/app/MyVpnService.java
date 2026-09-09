package com.myvpn.app;

import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import org.json.JSONArray;
import org.json.JSONObject;
import libXray.DialerController;
import libXray.LibXray;

public class MyVpnService extends VpnService {
    public static final String ACTION_STOP = "com.myvpn.app.STOP_VPN";
    private static final String CHANNEL_ID = "myvpn_vpn";
    private static final int NOTIFICATION_ID = 1001;
    private ParcelFileDescriptor vpnInterface;
    private boolean xrayStarted = false;

    private final DialerController dialerController = new DialerController() {
        @Override
        public boolean protectFd(long fd) {
            return MyVpnService.this.protect((int) fd);
        }
    };

    @Override
    public void onCreate() {
        android.util.Log.e("MYVPN_SERVICE", "onCreate ENTERED");
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        android.util.Log.e("MyVPN_SERVICE", "onStartCommand ENTERED");
        startForeground(NOTIFICATION_ID, buildNotification());

        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopVpnInternal();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (xrayStarted) return START_NOT_STICKY;

        String vlessLink = intent != null ? intent.getStringExtra("vlessLink") : "";
        if (vlessLink == null || vlessLink.isEmpty()) {
            vlessLink = getSharedPreferences("MyVPN", MODE_PRIVATE).getString("vlessLink", "");
        }
        if (vlessLink == null || vlessLink.isEmpty()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            vpnInterface = new Builder()
                    .setSession("MyVPN")
                    .setMtu(1500)
                    .addAddress("10.8.0.2", 32)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    .establish();

            if (vpnInterface == null) throw new Exception("Failed to establish VPN interface");

            LibXray.registerDialerController(dialerController);
                LibXray.setDNS(dialerController, "1.1.1.1:53");

            JSONObject request = new JSONObject()
                    .put("apiVersion", 1)
                    .put("method", "convertShareLinksToXrayJson")
                    .put("payload", new JSONObject().put("text", vlessLink));

            JSONObject response = new JSONObject(LibXray.invoke(request.toString()));
            if (!response.optBoolean("success", false)) {
                throw new Exception("VLESS convert failed: " + response.optString("error", "unknown error"));
            }

            JSONObject config = response.getJSONObject("data");
            removeSendThrough(config);
            normalizeRealityClient(config);
JSONArray outbounds = config.getJSONArray("outbounds");
if (outbounds.length() == 0) throw new Exception("No VLESS outbound found");
for (int i = 0; i < outbounds.length(); i++) {
    outbounds.getJSONObject(i).put("tag", i == 0 ? "vless-out" : "outbound-" + i);
}
            JSONObject tunInbound = new JSONObject()
                    .put("tag", "tun-in")
                    .put("protocol", "tun")
                    .put("settings", new JSONObject()
                            .put("name", "myvpn0")
                            .put("mtu", 1500)
                            .put("gateway", new JSONArray().put("10.8.0.1"))
                            .put("dns", new JSONArray().put("1.1.1.1")));
            config.put("inbounds", new JSONArray().put(tunInbound));

            JSONObject rule = new JSONObject()
                    .put("type", "field")
                    .put("inboundTag", new JSONArray().put("tun-in"))
                    .put("outboundTag", "vless-out");
            config.put("routing", new JSONObject()
                    .put("domainStrategy", "AsIs")
                    .put("rules", new JSONArray().put(rule)));

            try {
    ContentValues cv = new ContentValues();
    cv.put(MediaStore.Downloads.DISPLAY_NAME, "myvpn_xray_config.json");
    cv.put(MediaStore.Downloads.MIME_TYPE, "application/json");
    cv.put(MediaStore.Downloads.IS_PENDING, 1);
    android.net.Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
    if (u != null) {
        try (OutputStream os = getContentResolver().openOutputStream(u)) {
            if (os != null) os.write(config.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        cv.clear();
        cv.put(MediaStore.Downloads.IS_PENDING, 0);
        getContentResolver().update(u, cv, null, null);
    }
} catch (Exception ignored) {}

JSONObject runRequest = new JSONObject()
                    .put("apiVersion", 1)
                    .put("method", "runXrayFromJson")
                    .put("payload", new JSONObject()
                            .put("configJSON", config.toString())
                            .put("tunFD", vpnInterface.getFd()));

            JSONObject result = new JSONObject(LibXray.invoke(runRequest.toString()));
            if (!result.optBoolean("success", false)) {
                throw new Exception("Xray start failed: " + result.optString("error", "unknown error"));
            }
            xrayStarted = true;
            getSharedPreferences("MyVPN", MODE_PRIVATE)
                    .edit()
                    .putBoolean("vpnRunning", true)
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
            try { java.io.FileOutputStream f = openFileOutput("xray_error.txt", MODE_PRIVATE); String fullError = "CLASS=" + e.getClass().getName() + "\nMESSAGE=" + e.getMessage() + "\nTOSTRING=" + e.toString(); f.write(fullError.getBytes(java.nio.charset.StandardCharsets.UTF_8)); getSharedPreferences("MyVPN", MODE_PRIVATE).edit().putString("xrayError", fullError).apply(); try { android.content.ContentValues cv = new android.content.ContentValues(); cv.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, "xray_error.txt"); cv.put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain"); cv.put(android.provider.MediaStore.Downloads.IS_PENDING, 1); android.net.Uri u = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv); if (u != null) { try (java.io.OutputStream os = getContentResolver().openOutputStream(u)) { os.write(fullError.getBytes(java.nio.charset.StandardCharsets.UTF_8)); } cv.clear(); cv.put(android.provider.MediaStore.Downloads.IS_PENDING, 0); getContentResolver().update(u, cv, null, null); } } catch (Exception ignored) {} f.close(); } catch (Exception ignored) {}
            android.widget.Toast.makeText(getApplicationContext(), "VPN ERROR: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            try { LibXray.invoke("{\"apiVersion\":1,\"method\":\"stopXray\"}"); } catch (Exception ignored) {}
            if (vpnInterface != null) {
                try { vpnInterface.close(); } catch (Exception ignored) {}
                vpnInterface = null;
            }
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }

    private void normalizeRealityClient(JSONObject config) throws Exception {
        JSONArray outbounds = config.optJSONArray("outbounds");
        if (outbounds == null) return;

        for (int i = 0; i < outbounds.length(); i++) {
            JSONObject outbound = outbounds.optJSONObject(i);
            if (outbound == null) continue;

            JSONObject stream = outbound.optJSONObject("streamSettings");
            if (stream == null) continue;

            if (!"reality".equalsIgnoreCase(stream.optString("security", ""))) {
                continue;
            }

            JSONObject reality = stream.optJSONObject("realitySettings");
            if (reality == null) continue;

            JSONArray serverNames = reality.optJSONArray("serverNames");
            String serverName = reality.optString("serverName", "");

            if (serverName.isEmpty() && serverNames != null && serverNames.length() > 0) {
                String firstServerName = serverNames.optString(0, "");
                if (!firstServerName.isEmpty()) {
                    reality.put("serverName", firstServerName);
                }
            }

            JSONArray shortIds = reality.optJSONArray("shortIds");
            String shortId = reality.optString("shortId", "");

            if (shortId.isEmpty() && shortIds != null && shortIds.length() > 0) {
                String firstShortId = shortIds.optString(0, "");
                if (!firstShortId.isEmpty()) {
                    reality.put("shortId", firstShortId);
                }
            }

            // These are server-side / incompatible fields for a client Reality config.
            reality.remove("privateKey");
            reality.remove("serverNames");
            reality.remove("shortIds");
            reality.remove("masterKeyLog");
            reality.remove("target");
            reality.remove("dest");
            reality.remove("type");
            reality.remove("xver");
            reality.remove("mldsa65Seed");
            reality.remove("mldsa65Verify");
            reality.remove("limitFallbackUpload");
            reality.remove("limitFallbackDownload");
        }
    }

    private void removeSendThrough(Object value) throws Exception {
        if (value instanceof JSONObject) {
            JSONObject obj = (JSONObject) value;
            obj.remove("sendThrough");
            java.util.Iterator<String> keys = obj.keys();
            java.util.ArrayList<String> list = new java.util.ArrayList<>();
            while (keys.hasNext()) list.add(keys.next());
            for (String key : list) removeSendThrough(obj.opt(key));
        } else if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            for (int i = 0; i < arr.length(); i++) {
                removeSendThrough(arr.opt(i));
            }
        }
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("MyVPN")
                .setContentText("VPN service is running")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "MyVPN VPN", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void stopVpnInternal() {
        try {
            LibXray.invoke("{\"apiVersion\":1,\"method\":\"stopXray\"}");
        } catch (Exception ignored) {}

        try {
            LibXray.resetDNS();
        } catch (Exception ignored) {}

        xrayStarted = false;
        getSharedPreferences("MyVPN", MODE_PRIVATE)
                .edit()
                .putBoolean("vpnRunning", false)
                .apply();

        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (Exception ignored) {}
            vpnInterface = null;
        }

        try {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        stopVpnInternal();
        super.onDestroy();
    }

}
