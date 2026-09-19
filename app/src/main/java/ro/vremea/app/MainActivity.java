package ro.vremea.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity {
    private static final int LOCATION_REQUEST = 42;
    private static final int BG = Color.rgb(15, 25, 35);
    private static final int CARD = Color.rgb(26, 39, 51);
    private static final int CARD2 = Color.rgb(31, 48, 64);
    private static final int ACCENT = Color.rgb(79, 195, 247);
    private static final int MUTED = Color.rgb(122, 155, 181);
    private static final int TEXT = Color.rgb(232, 244, 253);
    private static final int BORDER = Color.rgb(42, 63, 82);

    private LinearLayout root, content, tabs;
    private TextView clock, coords, status;
    private EditText latInput, lonInput;
    private SharedPreferences preferences;
    private double latitude, longitude;
    private boolean hasLocation;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clockTask = new Runnable() {
        @Override public void run() {
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            format.setTimeZone(TimeZone.getTimeZone("Europe/Bucharest"));
            clock.setText(format.format(new java.util.Date()));
            handler.postDelayed(this, 1000);
        }
    };

    private static final String[] ICONS = {"☀️","🌤️","⛅","☁️","🌫️","🌫️","🌦️","🌦️","🌧️","🌧️","🌧️","🌨️","🌨️","❄️","🌨️","🌦️","🌧️","⛈️","🌨️","❄️","⛈️","⛈️","⛈️"};
    private static final String[] DESCRIPTIONS = {"Cer senin","Predominant senin","Parțial noros","Înnorat","Ceață","Ceață cu chiciură","Burnițe ușoare","Burnițe moderate","Burnițe intense","Ploaie ușoară","Ploaie moderată","Ploaie puternică","Ninsoare ușoară","Ninsoare moderată","Ninsoare abundentă","Fulgi de gheață","Averse ușoare","Averse moderate","Averse puternice","Averse de ninsoare","Averse abundente de ninsoare","Furtună","Furtună cu grindină"};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences("weather", MODE_PRIVATE);
        buildUi();
        handler.post(clockTask);
        if (preferences.contains("lat") && preferences.contains("lon")) {
            latitude = Double.longBitsToDouble(preferences.getLong("lat", 0));
            longitude = Double.longBitsToDouble(preferences.getLong("lon", 0));
            hasLocation = true;
            updateLocationText();
            fetchWeather();
        } else {
            showSettings();
        }
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(16), dp(10), dp(16), dp(8));
        header.setBackgroundColor(CARD);
        LinearLayout top = row();
        clock = label("--:--:--", 17, ACCENT);
        top.addView(clock, weight(1));
        top.addView(button("⟳ Actualizare", v -> refreshWeather()));
        header.addView(top);
        LinearLayout locationRow = row();
        coords = label("Nicio locație salvată", 11, MUTED);
        locationRow.addView(coords, weight(1));
        locationRow.addView(button("⚙", v -> showSettings()));
        header.addView(locationRow);
        root.addView(header);

        tabs = row();
        tabs.setBackgroundColor(CARD);
        tabs.addView(tab("Acum", "current"), weight(1));
        tabs.addView(tab("Ore", "hourly"), weight(1));
        tabs.addView(tab("Zile", "daily"), weight(1));
        tabs.addView(tab("Locație", "settings"), weight(1));
        root.addView(tabs);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(24));
        scroll.addView(content);
        root.addView(scroll, fillRemaining());
        setContentView(root);
    }

    private View tab(String text, final String name) {
        Button b = button(text, v -> {
            if (name.equals("settings")) showSettings();
            else if (name.equals("current")) fetchWeather();
            else showForecast(name);
        });
        b.setTextSize(12);
        return b;
    }

    private void showSettings() {
        content.removeAllViews();
        content.addView(label("LOCAȚIE GPS", 12, MUTED));
        LinearLayout inputs = row();
        latInput = input(hasLocation ? String.format(Locale.US, "%.6f", latitude) : "", "Latitudine");
        lonInput = input(hasLocation ? String.format(Locale.US, "%.6f", longitude) : "", "Longitudine");
        inputs.addView(latInput, weight(1));
        inputs.addView(lonInput, weight(1));
        content.addView(inputs);
        content.addView(fullButton("💾 Salvează locația", v -> saveLocation()));
        content.addView(fullButton("📍 Detectează automat", v -> detectLocation()));
        status = label("", 13, ACCENT);
        status.setGravity(Gravity.CENTER);
        content.addView(status);
        content.addView(label("\nINFORMAȚII\n\nDate furnizate de Open-Meteo (API gratuit, fără cheie).\nLocația este salvată local pe dispozitiv.\nActualizarea se face la reîncărcarea aplicației sau manual.", 13, MUTED));
    }

    private void saveLocation() {
        try {
            double lat = Double.parseDouble(latInput.getText().toString());
            double lon = Double.parseDouble(lonInput.getText().toString());
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) throw new NumberFormatException();
            setLocation(lat, lon);
            showToast("Locație salvată");
            fetchWeather();
        } catch (NumberFormatException e) {
            status.setText("Coordonate invalide.");
            status.setTextColor(Color.rgb(239, 154, 154));
        }
    }

    private void detectLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            status.setText("Se detectează...");
            manager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                @Override public void onLocationChanged(Location location) {
                    setLocation(location.getLatitude(), location.getLongitude());
                    showToast("Locație detectată și salvată");
                    fetchWeather();
                }
                @Override public void onProviderDisabled(String provider) { status.setText("Activați locația dispozitivului."); }
            }, Looper.getMainLooper());
        } catch (SecurityException e) {
            status.setText("Permisiunea pentru locație este necesară.");
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == LOCATION_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) detectLocation();
        else if (status != null) status.setText("Permisiunea pentru locație a fost refuzată.");
    }

    private void setLocation(double lat, double lon) {
        latitude = lat; longitude = lon; hasLocation = true;
        preferences.edit().putLong("lat", Double.doubleToRawLongBits(lat)).putLong("lon", Double.doubleToRawLongBits(lon)).apply();
        updateLocationText();
    }

    private void updateLocationText() {
        String ns = latitude >= 0 ? "N" : "S", ew = longitude >= 0 ? "E" : "V";
        coords.setText(String.format(Locale.US, "%.5f°%s  %.5f°%s", Math.abs(latitude), ns, Math.abs(longitude), ew));
    }

    private void refreshWeather() {
        if (!hasLocation) { showSettings(); return; }
        fetchWeather();
    }

    private void fetchWeather() {
        if (!hasLocation) return;
        showMessage("Se încarcă datele...", false);
        new Thread(() -> {
            try {
                String url = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude
                        + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m,cloud_cover,visibility"
                        + "&hourly=temperature_2m,weather_code,precipitation_probability,precipitation"
                        + "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_sum,precipitation_probability_max"
                        + "&wind_speed_unit=kmh&precipitation_unit=mm&timezone=Europe%2FBucharest&forecast_days=5";
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(10000); c.setReadTimeout(15000);
                if (c.getResponseCode() != 200) throw new Exception("Eroare API: " + c.getResponseCode());
                BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder result = new StringBuilder(); String line;
                while ((line = reader.readLine()) != null) result.append(line);
                JSONObject data = new JSONObject(result.toString());
                runOnUiThread(() -> renderCurrent(data));
            } catch (Exception e) {
                runOnUiThread(() -> showMessage("⚠️ " + e.getMessage() + "\n\nVerificați conexiunea și coordonatele.", true));
            }
        }).start();
    }

    private void renderCurrent(JSONObject data) {
        content.removeAllViews();
        try {
            JSONObject c = data.getJSONObject("current");
            double temp = c.getDouble("temperature_2m");
            TextView hero = label(icon(c.getInt("weather_code")) + "\n" + format(temp) + "°\n" + description(c.getInt("weather_code"))
                    + "\nSenzație termică: " + format(c.getDouble("apparent_temperature")) + "°C", 20, TEXT);
            hero.setGravity(Gravity.CENTER);
            hero.setPadding(0, dp(20), 0, dp(20));
            content.addView(hero);
            addMetric("💧 Umiditate", c.getInt("relative_humidity_2m") + "%");
            addMetric("💨 Vânt", format(c.getDouble("wind_speed_10m")) + " km/h · " + windDirection(c.getDouble("wind_direction_10m")));
            addMetric("🌡️ Presiune", Math.round(c.getDouble("surface_pressure")) + " hPa");
            addMetric("☁️ Acoperire nori", c.getInt("cloud_cover") + "%");
            addMetric("👁️ Vizibilitate", visibility(c.getDouble("visibility")));
            addMetric("🌧️ Precipitații", format(c.getDouble("precipitation")) + " mm");
            content.addView(fullButton("Ore", v -> showForecast("hourly")));
            content.addView(fullButton("Zile", v -> showForecast("daily")));
        } catch (Exception e) { showMessage("Nu s-au putut afișa datele meteo.", true); }
    }

    private void showForecast(String type) {
        if (!hasLocation) { showSettings(); return; }
        showMessage("Se încarcă...", false);
        new Thread(() -> {
            try {
                // Forecast data is fetched again to keep the screen correct after a tab change.
                String url = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude
                        + "&hourly=temperature_2m,weather_code,precipitation_probability&daily=weather_code,temperature_2m_max,temperature_2m_min"
                        + "&wind_speed_unit=kmh&timezone=Europe%2FBucharest&forecast_days=5";
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder b = new StringBuilder(); String line;
                while ((line = r.readLine()) != null) b.append(line);
                JSONObject d = new JSONObject(b.toString());
                runOnUiThread(() -> renderForecast(d, type));
            } catch (Exception e) { runOnUiThread(() -> showMessage("⚠️ " + e.getMessage(), true)); }
        }).start();
    }

    private void renderForecast(JSONObject d, String type) {
        content.removeAllViews();
        try {
            if (type.equals("hourly")) {
                JSONObject h = d.getJSONObject("hourly"); JSONArray times = h.getJSONArray("time");
                java.text.SimpleDateFormat hourFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
                hourFormat.setTimeZone(TimeZone.getTimeZone("Europe/Bucharest"));
                String now = hourFormat.format(new java.util.Date());
                int currentIndex = 0;
                for (int i = 0; i < times.length(); i++) {
                    if (times.getString(i).compareTo(now) <= 0) currentIndex = i;
                    else break;
                }
                for (int offset = 0; offset < 5 && currentIndex + offset < times.length(); offset++) {
                    int i = currentIndex + offset;
                    String title = (offset == 0 ? "▶ " : "") + times.getString(i).substring(11, 16);
                    addMetric(title, icon(h.getJSONArray("weather_code").getInt(i)) + "  " + format(h.getJSONArray("temperature_2m").getDouble(i)) + "°  💧" + h.getJSONArray("precipitation_probability").getInt(i) + "%");
                }
            } else {
                JSONObject day = d.getJSONObject("daily"); JSONArray times = day.getJSONArray("time");
                for (int i = 0; i < Math.min(5, times.length()); i++)
                    addMetric(i == 0 ? "Azi" : times.getString(i), icon(day.getJSONArray("weather_code").getInt(i)) + "  " + format(day.getJSONArray("temperature_2m_max").getDouble(i)) + "° / " + format(day.getJSONArray("temperature_2m_min").getDouble(i)) + "°");
            }
        } catch (Exception e) { showMessage("Nu s-au putut afișa prognozele.", true); }
    }

    private void addMetric(String title, String value) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14), dp(12), dp(14), dp(12)); card.setBackgroundColor(CARD);
        TextView t = label(title, 12, MUTED), v = label(value, 17, TEXT); card.addView(t); card.addView(v);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, 0, 0, dp(8)); content.addView(card, p);
    }

    private void showMessage(String message, boolean error) {
        content.removeAllViews(); TextView t = label(message, 16, error ? Color.rgb(239,154,154) : MUTED); t.setGravity(Gravity.CENTER); t.setPadding(0, dp(50), 0, dp(50)); content.addView(t);
    }

    private String icon(int code) { int index = code == 0 ? 0 : code <= 3 ? code : code < 50 ? 4 : code < 60 ? 6 : code < 70 ? 9 : code < 80 ? 12 : code < 90 ? 16 : 21; return ICONS[index]; }
    private String description(int code) { int index = code == 0 ? 0 : code <= 3 ? code : code < 50 ? 4 : code < 60 ? 6 : code < 70 ? 9 : code < 80 ? 12 : code < 90 ? 16 : 21; return DESCRIPTIONS[index]; }
    private String format(double value) { return String.format(Locale.US, "%.1f", value); }
    private String visibility(double m) { return m >= 1000 ? format(m / 1000) + " km" : Math.round(m) + " m"; }
    private String windDirection(double degrees) { return new String[]{"N","NE","E","SE","S","SV","V","NV"}[(int)Math.round(degrees / 45) % 8]; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private TextView label(String text, int size, int color) { TextView t = new TextView(this); t.setText(text); t.setTextSize(size); t.setTextColor(color); t.setPadding(0, dp(3), 0, dp(3)); return t; }
    private Button button(String text, View.OnClickListener listener) { Button b = new Button(this); b.setText(text); b.setTextColor(ACCENT); b.setTextSize(12); b.setOnClickListener(listener); return b; }
    private Button fullButton(String text, View.OnClickListener listener) { Button b = button(text, listener); b.setAllCaps(false); b.setLayoutParams(new LinearLayout.LayoutParams(-1, -2)); return b; }
    private EditText input(String value, String hint) { EditText e = new EditText(this); e.setText(value); e.setHint(hint); e.setHintTextColor(MUTED); e.setTextColor(TEXT); e.setTextSize(14); e.setInputType(2 | 8192); return e; }
    private LinearLayout.LayoutParams weight(float w) { return new LinearLayout.LayoutParams(0, -2, w); }
    private LinearLayout.LayoutParams fillRemaining() { return new LinearLayout.LayoutParams(-1, 0, 1); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void showToast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    @Override protected void onDestroy() { handler.removeCallbacks(clockTask); super.onDestroy(); }
}
