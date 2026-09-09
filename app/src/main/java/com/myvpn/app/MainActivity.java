package com.myvpn.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.VpnService;
import android.net.TrafficStats;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONObject;

import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int VPN_REQUEST_CODE = 1001;
    private static final String PREFS = "MyVPN";
    private final ExecutorService geoExecutor =
            Executors.newSingleThreadExecutor();

    private final android.os.Handler uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable uiStateUpdater = new Runnable() {
        @Override
        public void run() {
            refreshConnectionState();
            uiHandler.postDelayed(this, 500);
        }
    };

    private EditText remarks;
    private EditText location;
    private EditText address;
    private EditText port;
    private EditText id;
    private EditText encryption;
    private EditText flow;
    private EditText network;
    private EditText header;
    private EditText security;
    private EditText sni;
    private EditText fingerprint;
    private EditText publicKey;
    private EditText shortId;
    private EditText spiderX;

    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildUI();
        }

    private LinearLayout root;
    private LinearLayout accountList;
    private TextView accountNameText;
    private TextView connectButton;

private TextView durationText;
private TextView downloadSpeedText;
private TextView uploadSpeedText;

private long speedLastRx = -1;
private long speedLastTx = -1;
private long speedLastTime = -1;
private long vpnStartTime = -1;

private final android.os.Handler speedHandler =
        new android.os.Handler(android.os.Looper.getMainLooper());

private final Runnable speedUpdater = new Runnable() {
    @Override
    public void run() {
        updateSpeedUI();
        speedHandler.postDelayed(this, 1000);
    }
};

    private void buildUI() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF);
        root.setPadding(28, 24, 28, 24);

        // -------------------------
        // TOP BAR
        // -------------------------

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("MyVPN");
        title.setTextColor(0xFF000000);
        title.setTextSize(28);
        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        topBar.addView(
                title,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(topBar);

        // -------------------------
        // VLESS ACCOUNT BUTTON
        // -------------------------

        accountList = new LinearLayout(this);
        accountList.setOrientation(LinearLayout.VERTICAL);
        accountList.setPadding(0, 24, 0, 0);

        TextView vlessButton = new TextView(this);

        vlessButton.setText("VLESS");
        vlessButton.setTextColor(0xFF000000);
        vlessButton.setTextSize(19);
        vlessButton.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        vlessButton.setGravity(Gravity.CENTER_VERTICAL);
        vlessButton.setPadding(24, 22, 24, 22);

        android.graphics.drawable.GradientDrawable vlessBg =
                new android.graphics.drawable.GradientDrawable();

        vlessBg.setColor(0xFFF0F0F0);
        vlessBg.setCornerRadius(22);

        vlessButton.setBackground(vlessBg);

        accountList.addView(
                vlessButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(accountList);

        // -------------------------
        // STATUS
        // -------------------------

        statusText = new TextView(this);

        statusText.setText("Disconnected");
        statusText.setTextColor(0xFF000000);
        statusText.setTextSize(15);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 14, 0, 14);

        root.addView(statusText);

        // -------------------------
        // CONNECT BUTTON
        // -------------------------

        connectButton = new TextView(this);

        connectButton.setText("OFF");
        connectButton.setTextColor(0xFFFFFFFF);
        connectButton.setTextSize(22);

        connectButton.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        connectButton.setGravity(Gravity.CENTER);

        android.graphics.drawable.GradientDrawable offBg =
                new android.graphics.drawable.GradientDrawable();

        offBg.setShape(
                android.graphics.drawable.GradientDrawable.OVAL
        );

        offBg.setColor(0xFFE53935);

        connectButton.setBackground(offBg);

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        190,
                        190
                );

        buttonParams.gravity =
                Gravity.CENTER_HORIZONTAL;

        buttonParams.topMargin = 55;
        buttonParams.bottomMargin = 20;

        root.addView(
                connectButton,
                buttonParams
        );

        // -------------------------
        // ACCOUNT NAME
        // -------------------------

        accountNameText = new TextView(this);

        accountNameText.setText("");
        accountNameText.setTextColor(0xFF000000);
        accountNameText.setTextSize(18);

        accountNameText.setGravity(Gravity.CENTER);

        accountNameText.setPadding(
                0,
                25,
                0,
                10
        );

        root.addView(
                accountNameText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        // -------------------------
        // SPEED CARD
        // -------------------------

        LinearLayout speedCard =
                new LinearLayout(this);

        speedCard.setOrientation(
                LinearLayout.VERTICAL
        );

        speedCard.setGravity(Gravity.CENTER);

        speedCard.setPadding(
                20,
                18,
                20,
                18
        );

        android.graphics.drawable.GradientDrawable speedBg =
                new android.graphics.drawable.GradientDrawable();

        speedBg.setColor(0xFFF5F5F5);
        speedBg.setCornerRadius(24);

        speedCard.setBackground(speedBg);

        durationText = new TextView(this);

        durationText.setText("00:00:00");
        durationText.setTextColor(0xFF000000);
        durationText.setTextSize(17);
        durationText.setGravity(Gravity.CENTER);

        downloadSpeedText = new TextView(this);

        downloadSpeedText.setText("↓ 0 Mbps");
        downloadSpeedText.setTextColor(0xFF000000);
        downloadSpeedText.setTextSize(18);
        downloadSpeedText.setGravity(Gravity.CENTER);

        uploadSpeedText = new TextView(this);

        uploadSpeedText.setText("↑ 0 Mbps");
        uploadSpeedText.setTextColor(0xFF000000);
        uploadSpeedText.setTextSize(18);
        uploadSpeedText.setGravity(Gravity.CENTER);

        speedCard.addView(durationText);
        speedCard.addView(downloadSpeedText);
        speedCard.addView(uploadSpeedText);

        LinearLayout.LayoutParams speedParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        speedParams.topMargin = 20;

        root.addView(
                speedCard,
                speedParams
        );

        // -------------------------
        // SHOW UI FIRST
        // -------------------------

        setContentView(root);

        // -------------------------
        // VLESS CLICK
        // -------------------------

        vlessButton.setOnClickListener(v -> {

            SharedPreferences p =
                    getSharedPreferences(
                            PREFS,
                            MODE_PRIVATE
                    );

            String savedLink =
                    p.getString(
                            "vlessLink",
                            ""
                    );

            showVlessEditor(savedLink);
        });

        // -------------------------
        // CONNECT CLICK
        // -------------------------

        connectButton.setOnClickListener(v -> {

            SharedPreferences p =
                    getSharedPreferences(
                            PREFS,
                            MODE_PRIVATE
                    );

            String savedLink =
                    p.getString(
                            "vlessLink",
                            ""
                    );

            if (savedLink == null ||
                    savedLink.trim().isEmpty()) {

                Toast.makeText(
                        this,
                        "Tap VLESS and add configuration first",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            boolean running =
                    p.getBoolean(
                            "vpnRunning",
                            false
                    );

            if (running) {

                disconnectVPN();
                setDisconnectedUI();

            } else {

                connectVPN();
            }
        });

        // Load only after ALL UI views exist
        loadSavedConfig();

        refreshConnectionState();
    }

    private void showVlessEditor(String currentLink) {

        ScrollView scroll = new ScrollView(this);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(32, 24, 32, 32);
        form.setBackgroundColor(0xFFFFFFFF);

        scroll.addView(form);

        TextView title = new TextView(this);
        title.setText("VLESS");
        title.setTextColor(0xFF000000);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        form.addView(title);

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);

        Button paste = new Button(this);
        paste.setText("PASTE VLESS");
        paste.setTextColor(0xFF000000);

        Button copy = new Button(this);
        copy.setText("COPY VLESS");
        copy.setTextColor(0xFF000000);

        buttonRow.addView(
                paste,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        buttonRow.addView(
                copy,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        form.addView(buttonRow);

        TextView configTitle = new TextView(this);
        configTitle.setText("CONFIG SETTINGS");
        configTitle.setTextColor(0xFF000000);
        configTitle.setTextSize(18);
        configTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        configTitle.setPadding(0, 25, 0, 10);
        form.addView(configTitle);

        remarks = field(form, "Remarks / Name");
        location = field(form, "Location (Auto Detect)");
        location.setFocusable(false);
        location.setCursorVisible(false);
        location.setTextColor(0xFF666666);
        address = field(form, "Address");
        port = field(form, "Port");
        id = field(form, "ID / UUID");
        encryption = field(form, "Encryption");
        flow = field(form, "Flow");
        network = field(form, "Network / Type");
        header = field(form, "Header");
        security = field(form, "Security");
        sni = field(form, "SNI");
        fingerprint = field(form, "Fingerprint");
        publicKey = field(form, "Public Key");
        shortId = field(form, "Short ID");
        spiderX = field(form, "SpiderX");

        Button save = new Button(this);
        save.setText("SAVE");
        save.setTextColor(0xFF000000);
        form.addView(save);

        Button delete = new Button(this);
        delete.setText("DELETE");
        delete.setTextColor(0xFFD32F2F);
        form.addView(delete);

        android.app.AlertDialog dialog =
                new android.app.AlertDialog.Builder(this)
                        .setView(scroll)
                        .create();

        dialog.setOnShowListener(d -> {

            if (currentLink != null &&
                    !currentLink.trim().isEmpty()) {

                loadVless(currentLink);

                SharedPreferences p =
                        getSharedPreferences(PREFS, MODE_PRIVATE);

                if (remarks != null) {
                    remarks.setText(
                            p.getString("remarks",
                                    remarks.getText().toString())
                    );
                }

                if (location != null) {
                    location.setText(
                            p.getString("location", "")
                    );
                }
            }

            paste.setOnClickListener(v -> pasteVless());

            copy.setOnClickListener(v -> {

                String link;

                try {
                    link = buildVlessLink();
                } catch (Exception e) {
                    Toast.makeText(
                            this,
                            "VLESS data is incomplete",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                if (link.trim().isEmpty() ||
                        link.equals("vless://@: ?")) {

                    Toast.makeText(
                            this,
                            "Nothing to copy",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                ClipboardManager clipboard =
                        (ClipboardManager)
                                getSystemService(CLIPBOARD_SERVICE);

                if (clipboard != null) {
                    clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                    "VLESS",
                                    link
                            )
                    );

                    Toast.makeText(
                            this,
                            "VLESS copied",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
            save.setOnClickListener(v -> {

                String addressValue =
                        address.getText().toString().trim();

                String uuidValue =
                        id.getText().toString().trim();

                if (addressValue.isEmpty() ||
                        uuidValue.isEmpty()) {

                    Toast.makeText(
                            this,
                            "Address and UUID are required",
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                save.setEnabled(false);

                if (location != null) {
                    location.setText("Detecting country...");
                }

                detectCountry(addressValue, (country, countryCode) -> {

                    SharedPreferences.Editor geoEditor =
                            getSharedPreferences(PREFS, MODE_PRIVATE).edit();

                    geoEditor.putString(
                            "location",
                            country == null || country.trim().isEmpty()
                                    ? "Unknown"
                                    : country.trim()
                    );

                    geoEditor.putString(
                            "countryCode",
                            countryCode == null
                                    ? ""
                                    : countryCode.trim().toUpperCase()
                    );

                    geoEditor.apply();

                    if (location != null) {
                        location.setText(
                                country == null || country.trim().isEmpty()
                                        ? "Unknown"
                                        : country.trim()
                        );
                    }

                    saveConfig();
                    refreshAccountUI();
                    refreshConnectionState();

                    Toast.makeText(
                            this,
                            "VLESS saved",
                            Toast.LENGTH_SHORT
                    ).show();

                    dialog.dismiss();
                });
            });

            delete.setOnClickListener(v -> {

                new android.app.AlertDialog.Builder(this)
                        .setTitle("Delete VLESS?")
                        .setMessage(
                                "This account will be removed."
                        )
                        .setNegativeButton(
                                "CANCEL",
                                null
                        )
                        .setPositiveButton(
                                "DELETE",
                                (dd, which) -> {

                                    disconnectVPN();

                                    SharedPreferences.Editor editor =
                                            getSharedPreferences(
                                                    PREFS,
                                                    MODE_PRIVATE
                                            ).edit();

                                    editor.remove("vlessLink");
                                    editor.remove("remarks");
                                    editor.remove("location");
                                    editor.remove("countryCode");
                                    editor.remove("address");
                                    editor.remove("port");
                                    editor.remove("id");
                                    editor.remove("encryption");
                                    editor.remove("flow");
                                    editor.remove("network");
                                    editor.remove("header");
                                    editor.remove("security");
                                    editor.remove("sni");
                                    editor.remove("fingerprint");
                                    editor.remove("publicKey");
                                    editor.remove("shortId");
                                    editor.remove("spiderX");
                                    editor.putBoolean("vpnPending", false);
                                    editor.putBoolean("vpnRunning", false);
                                    editor.apply();

                                    clearConfigFields();
                                    refreshAccountUI();
                                    setDisconnectedUI();

                                    dialog.dismiss();
                                })
                        .show();
            });
        });

        dialog.show();
    }

    private String flagFromCountryCode(String countryCode) {
        if (countryCode == null) return "";

        String code = countryCode.trim().toUpperCase();

        if (code.length() != 2) return "";

        char first = code.charAt(0);
        char second = code.charAt(1);

        if (first < 'A' || first > 'Z' ||
                second < 'A' || second > 'Z') {
            return "";
        }

        return new String(
                new int[] {
                        0x1F1E6 + (first - 'A'),
                        0x1F1E6 + (second - 'A')
                },
                0,
                2
        ) + " ";
    }

    private void detectCountry(
            String host,
            java.util.function.BiConsumer<String, String> callback
    ) {
        geoExecutor.execute(() -> {

            String country = "";
            String countryCode = "";
            HttpURLConnection conn = null;

            try {
                InetAddress resolved =
                        InetAddress.getByName(host);

                String ip =
                        resolved.getHostAddress();

                URL url =
                        new URL("https://ipwho.is/" + ip);

                conn =
                        (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setUseCaches(false);

                if (conn.getResponseCode() ==
                        HttpURLConnection.HTTP_OK) {

                    StringBuilder jsonText =
                            new StringBuilder();

                    try (BufferedReader reader =
                                 new BufferedReader(
                                         new InputStreamReader(
                                                 conn.getInputStream(),
                                                 StandardCharsets.UTF_8
                                         )
                                 )) {

                        String line;

                        while ((line = reader.readLine()) != null) {
                            jsonText.append(line);
                        }
                    }

                    JSONObject obj =
                            new JSONObject(
                                    jsonText.toString()
                            );

                    if (obj.optBoolean("success", false)) {

                        country =
                                obj.optString(
                                        "country",
                                        ""
                                ).trim();

                        countryCode =
                                obj.optString(
                                        "country_code",
                                        ""
                                ).trim()
                                .toUpperCase();
                    }
                }

            } catch (Exception ignored) {
                // Unknown when GeoIP lookup fails.
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            String finalCountry = country;
            String finalCountryCode = countryCode;

            runOnUiThread(() ->
                    callback.accept(
                            finalCountry,
                            finalCountryCode
                    )
            );
        });
    }

    private void showExistingAccount() {
        SharedPreferences p =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        String link =
                p.getString("vlessLink", "").trim();

        showVlessEditor(link);
    }

    private void refreshAccountUI() {
        if (accountList == null) return;

        accountList.removeAllViews();
        accountList.setVisibility(android.view.View.VISIBLE);

        SharedPreferences p =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        String link =
                p.getString("vlessLink", "").trim();

        // VLESS row MUST always remain visible.
        TextView vlessRow = new TextView(this);
        vlessRow.setText("VLESS");
        vlessRow.setTextColor(0xFF000000);
        vlessRow.setTextSize(19);
        vlessRow.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        vlessRow.setGravity(Gravity.CENTER_VERTICAL);
        vlessRow.setPadding(24, 20, 24, 20);

        android.graphics.drawable.GradientDrawable vlessBg =
                new android.graphics.drawable.GradientDrawable();

        vlessBg.setColor(0xFFF0F0F0);
        vlessBg.setCornerRadius(22);
        vlessRow.setBackground(vlessBg);

        accountList.addView(
                vlessRow,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        vlessRow.setOnClickListener(v ->
                showVlessEditor(link)
        );

        // No saved account: keep only VLESS row.
        if (link.isEmpty()) {

            if (accountNameText != null) {
                accountNameText.setText("");
                accountNameText.setOnClickListener(null);
            }

            return;
        }

        String name =
                p.getString("remarks", "").trim();

        String locationValue =
                p.getString("location", "").trim();

        String addressValue =
                p.getString("address", "").trim();

        if (name.isEmpty()) {
            name = addressValue.isEmpty()
                    ? "VLESS"
                    : addressValue;
        }

        String countryCode =
                p.getString("countryCode", "").trim();

        String flag =
                flagFromCountryCode(countryCode);

        // Backward compatibility for old saved accounts.
        if (flag.isEmpty()) {
            if (locationValue.equalsIgnoreCase("Thailand") ||
                    locationValue.equalsIgnoreCase("TH") ||
                    locationValue.equalsIgnoreCase("Thai")) {

                flag = "🇹🇭 ";

            } else if (
                    locationValue.equalsIgnoreCase("Singapore") ||
                    locationValue.equalsIgnoreCase("SG")) {

                flag = "🇸🇬 ";

            } else if (
                    locationValue.equalsIgnoreCase("Japan") ||
                    locationValue.equalsIgnoreCase("JP")) {

                flag = "🇯🇵 ";

            } else if (
                    locationValue.equalsIgnoreCase("United States") ||
                    locationValue.equalsIgnoreCase("USA") ||
                    locationValue.equalsIgnoreCase("US")) {

                flag = "🇺🇸 ";

            } else if (
                    locationValue.equalsIgnoreCase("Hong Kong") ||
                    locationValue.equalsIgnoreCase("HK")) {

                flag = "🇭🇰 ";
            }
        }

        String displayName;

        if (!locationValue.isEmpty()) {
            displayName =
                    flag + locationValue +
                    "\n" +
                    name;
        } else {
            displayName = name;
        }

        if (!addressValue.isEmpty()) {
            displayName +=
                    "\n" + addressValue;
        }

        if (accountNameText != null) {

            accountNameText.setText(displayName);

            accountNameText.setTextColor(
                    0xFF000000
            );

            accountNameText.setTextSize(17);

            accountNameText.setGravity(
                    Gravity.CENTER
            );

            accountNameText.setOnClickListener(
                    v -> showExistingAccount()
            );
        }

        if (p.getBoolean("vpnRunning", false)) {
            setConnectedUI();
        } else {
            setDisconnectedUI();
        }
    }

    private void refreshConnectionState() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (p.getBoolean("vpnRunning", false)) {
            setConnectedUI();
        } else {
            setDisconnectedUI();
        }
    }

    private void setConnectedUI() {
        if (connectButton == null) return;

        connectButton.setText("ON");

        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(0xFF2EBD59);
        connectButton.setBackground(bg);

        statusText.setText("Connected");
        statusText.setTextColor(0xFF000000);
    }

    private void setDisconnectedUI() {
        if (connectButton == null) return;

        connectButton.setText("OFF");

        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(0xFFE53935);
        connectButton.setBackground(bg);

        statusText.setText("Disconnected");
        statusText.setTextColor(0xFF000000);
    }

    private void clearConfigFields() {
        if (remarks != null) remarks.setText("");
        if (address != null) address.setText("");
        if (port != null) port.setText("");
        if (id != null) id.setText("");
        if (encryption != null) encryption.setText("");
        if (flow != null) flow.setText("");
        if (network != null) network.setText("");
        if (header != null) header.setText("");
        if (security != null) security.setText("");
        if (sni != null) sni.setText("");
        if (fingerprint != null) fingerprint.setText("");
        if (publicKey != null) publicKey.setText("");
        if (shortId != null) shortId.setText("");
        if (spiderX != null) spiderX.setText("");
    }

    private EditText field(
            LinearLayout root,
            String hint
    ) {

        // LABEL
        TextView label = new TextView(this);

        label.setText(hint);
        label.setTextColor(0xFF000000);
        label.setTextSize(14);

        label.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        label.setPadding(
                0,
                16,
                0,
                4
        );

        root.addView(
                label,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );


        // INPUT
        EditText e = new EditText(this);

        e.setTextColor(0xFF000000);
        e.setHintTextColor(0xFF777777);

        e.setTextSize(16);

        e.setSingleLine(true);

        e.setInputType(
                InputType.TYPE_CLASS_TEXT
        );

        e.setPadding(
                12,
                10,
                12,
                10
        );

        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();

        bg.setColor(0xFFF5F5F5);
        bg.setCornerRadius(14);

        e.setBackground(bg);

        root.addView(
                e,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        return e;
    }

    private void pasteVless() {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();

            if (clip != null && clip.getItemCount() > 0) {
                CharSequence text = clip.getItemAt(0).coerceToText(this);

                if (text != null) {
                    String value = text.toString().trim();

                    if (value.startsWith("vless://")) {
                        loadVless(value);
                        statusText.setText("VLESS configuration loaded");
                        Toast.makeText(this, "VLESS pasted", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        }

        Toast.makeText(this, "Copy a VLESS link first", Toast.LENGTH_LONG).show();
    }

    private void loadVless(String link) {
        try {
            if (link == null || link.trim().isEmpty()) {
                return;
            }

            URI uri = URI.create(link.trim());

            String userInfo = uri.getUserInfo();

            if (id != null && userInfo != null) {
                id.setText(URLDecoder.decode(
                        userInfo,
                        StandardCharsets.UTF_8.name()));
            }

            if (address != null) {
                String host = uri.getHost();
                if (host != null) {
                    address.setText(host);
                }
            }

            if (port != null && uri.getPort() >= 0) {
                port.setText(String.valueOf(uri.getPort()));
            }

            String query = uri.getRawQuery();

            if (query != null && !query.isEmpty()) {
                String[] parts = query.split("&");

                for (String part : parts) {
                    if (part == null || part.isEmpty()) {
                        continue;
                    }

                    String[] kv = part.split("=", 2);
                    String key = kv[0];

                    String value = kv.length > 1
                            ? URLDecoder.decode(
                                    kv[1],
                                    StandardCharsets.UTF_8.name())
                            : "";

                    switch (key) {
                        case "encryption":
                            if (encryption != null)
                                encryption.setText(value);
                            break;

                        case "flow":
                            if (flow != null)
                                flow.setText(value);
                            break;

                        case "type":
                            if (network != null)
                                network.setText(value);
                            break;

                        case "headerType":
                            if (header != null)
                                header.setText(value);
                            break;

                        case "security":
                            if (security != null)
                                security.setText(value);
                            break;

                        case "sni":
                            if (sni != null)
                                sni.setText(value);
                            break;

                        case "fp":
                            if (fingerprint != null)
                                fingerprint.setText(value);
                            break;

                        case "pbk":
                            if (publicKey != null)
                                publicKey.setText(value);
                            break;

                        case "sid":
                            if (shortId != null)
                                shortId.setText(value);
                            break;

                        case "spx":
                            if (spiderX != null)
                                spiderX.setText(value);
                            break;
                    }
                }
            }

            if (remarks != null &&
                    remarks.getText().toString().trim().isEmpty() &&
                    address != null) {

                remarks.setText(address.getText().toString());
            }

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "VLESS parse failed: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void saveConfig() {
        SharedPreferences.Editor editor =
                getSharedPreferences(PREFS, MODE_PRIVATE).edit();

        editor.putString("remarks",
                remarks != null ? remarks.getText().toString().trim() : "");

        editor.putString("location",
                location != null ? location.getText().toString().trim() : "");

        editor.putString("address",
                address != null ? address.getText().toString().trim() : "");

        editor.putString("port",
                port != null ? port.getText().toString().trim() : "");

        editor.putString("id",
                id != null ? id.getText().toString().trim() : "");

        editor.putString("encryption",
                encryption != null ? encryption.getText().toString().trim() : "");

        editor.putString("flow",
                flow != null ? flow.getText().toString().trim() : "");

        editor.putString("network",
                network != null ? network.getText().toString().trim() : "");

        editor.putString("header",
                header != null ? header.getText().toString().trim() : "");

        editor.putString("security",
                security != null ? security.getText().toString().trim() : "");

        editor.putString("sni",
                sni != null ? sni.getText().toString().trim() : "");

        editor.putString("fingerprint",
                fingerprint != null ? fingerprint.getText().toString().trim() : "");

        editor.putString("publicKey",
                publicKey != null ? publicKey.getText().toString().trim() : "");

        editor.putString("shortId",
                shortId != null ? shortId.getText().toString().trim() : "");

        editor.putString("spiderX",
                spiderX != null ? spiderX.getText().toString().trim() : "");

        editor.putString("vlessLink", buildVlessLink());
        editor.apply();
    }

    private String buildVlessLink() {
        StringBuilder sb = new StringBuilder();

        sb.append("vless://");
        sb.append(id.getText().toString().trim());
        sb.append("@");
        sb.append(address.getText().toString().trim());
        sb.append(":");
        sb.append(port.getText().toString().trim());
        sb.append("?");

        addParam(sb, "security", security.getText().toString());
        addParam(sb, "encryption", encryption.getText().toString());
        addParam(sb, "pbk", publicKey.getText().toString());
        addParam(sb, "headerType", header.getText().toString());
        addParam(sb, "fp", fingerprint.getText().toString());
        addParam(sb, "spx", spiderX.getText().toString());
        addParam(sb, "type", network.getText().toString());
        addParam(sb, "sni", sni.getText().toString());
        addParam(sb, "sid", shortId.getText().toString());
        addParam(sb, "flow", flow.getText().toString());

        return sb.toString();
    }

    private void addParam(StringBuilder sb, String key, String value) {
        if (value == null || value.trim().isEmpty()) return;

        if (sb.charAt(sb.length() - 1) != '?') {
            sb.append("&");
        }

        sb.append(key);
        sb.append("=");

        try {
            sb.append(java.net.URLEncoder.encode(
                    value.trim(), StandardCharsets.UTF_8.name()
            ).replace("+", "%20"));
        } catch (Exception ignored) {
            sb.append(value.trim());
        }
    }

    private void loadSavedConfig() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);

        String savedLink = p.getString("vlessLink", "");

        if (!savedLink.isEmpty() && statusText != null) {
            statusText.setText("Saved configuration loaded");
        }
    }

    private void updateSpeedUI() {
        if (durationText == null ||
                downloadSpeedText == null ||
                uploadSpeedText == null) {
            return;
        }

        SharedPreferences p =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        boolean running =
                p.getBoolean("vpnRunning", false);

        if (!running) {
            vpnStartTime = -1;
            speedLastRx = -1;
            speedLastTx = -1;
            speedLastTime = -1;

            durationText.setText("00:00:00");
            downloadSpeedText.setText("↓ 0 Mbps");
            uploadSpeedText.setText("↑ 0 Mbps");
            return;
        }

        if (vpnStartTime < 0) {
            vpnStartTime = System.currentTimeMillis();
        }

        long elapsed =
                System.currentTimeMillis() - vpnStartTime;

        long totalSeconds = elapsed / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        durationText.setText(String.format(
                java.util.Locale.US,
                "%02d:%02d:%02d",
                hours, minutes, seconds));

        long rx = TrafficStats.getTotalRxBytes();
        long tx = TrafficStats.getTotalTxBytes();
        long now = System.currentTimeMillis();

        if (rx >= 0 && tx >= 0 &&
                speedLastRx >= 0 &&
                speedLastTx >= 0 &&
                speedLastTime > 0) {

            long dt = now - speedLastTime;

            if (dt > 0) {
                double downMbps =
                        ((rx - speedLastRx) * 8.0)
                        / (dt * 1000.0);

                double upMbps =
                        ((tx - speedLastTx) * 8.0)
                        / (dt * 1000.0);

                if (downMbps < 0) downMbps = 0;
                if (upMbps < 0) upMbps = 0;

                downloadSpeedText.setText(String.format(
                        java.util.Locale.US,
                        "↓ %.1f Mbps",
                        downMbps));

                uploadSpeedText.setText(String.format(
                        java.util.Locale.US,
                        "↑ %.1f Mbps",
                        upMbps));
            }
        }

        speedLastRx = rx;
        speedLastTx = tx;
        speedLastTime = now;
    }

    private void connectVPN() {
        SharedPreferences p =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        String config = p.getString("vlessLink", "").trim();

        if (config.isEmpty()) {
            Toast.makeText(this,
                    "Enter VLESS configuration first",
                    Toast.LENGTH_LONG).show();
            return;
        }

        p.edit()
                .putBoolean("vpnPending", true)
                .apply();

        Intent intent = VpnService.prepare(this);

        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            startVPNService(config);
        }
    }

    private void startVPNService(String config) {
        Intent intent = new Intent(this, MyVpnService.class);
        intent.putExtra("vlessLink", config);
        startForegroundService(intent);

        statusText.setText("VPN Connecting...");
    }

    private void disconnectVPN() {
        Intent stopIntent = new Intent(this, MyVpnService.class);
        stopIntent.setAction(MyVpnService.ACTION_STOP);
        startService(stopIntent);

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean("vpnPending", false)
                .apply();

        statusText.setText("Disconnected");
        Toast.makeText(this,
                "VPN Disconnected",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                SharedPreferences p =
                        getSharedPreferences(PREFS, MODE_PRIVATE);

                String config =
                        p.getString("vlessLink", "").trim();

                p.edit()
                        .putBoolean("vpnPending", false)
                        .apply();

                if (!config.isEmpty()) {
                    startVPNService(config);
                } else {
                    Toast.makeText(
                            this,
                            "Enter VLESS configuration first",
                            Toast.LENGTH_LONG).show();
                }

            } else {
                getSharedPreferences(PREFS, MODE_PRIVATE)
                        .edit()
                        .putBoolean("vpnPending", false)
                        .apply();

                Toast.makeText(this,
                        "VPN permission denied",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        uiHandler.removeCallbacks(uiStateUpdater);
        uiHandler.post(uiStateUpdater);

        speedHandler.removeCallbacks(speedUpdater);
        speedHandler.post(speedUpdater);

        SharedPreferences p =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        if (p.getBoolean("vpnPending", false)
                && VpnService.prepare(this) == null) {

            p.edit()
                    .putBoolean("vpnPending", false)
                    .apply();

            String config = p.getString("vlessLink", "");

            if (!config.isEmpty()) {
                startVPNService(config);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHandler.removeCallbacks(uiStateUpdater);
        speedHandler.removeCallbacks(speedUpdater);
    }
}
