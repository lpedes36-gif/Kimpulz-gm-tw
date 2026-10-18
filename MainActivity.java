package id.kimpul.tweaks;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.Gravity;
import android.widget.*;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import dev.rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private static final int REQUEST_SHIZUKU = 5102;
    private static final String[] KEYS = {
        "window_animation_scale",
        "transition_animation_scale",
        "animator_duration_scale"
    };
    private static final String[] LABELS = {
        "Window animation",
        "Transition animation",
        "Animator duration"
    };
    private static final String BACKUP_NONE = "__KIMPUL_VALUE_NOT_SET__";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView statusView, logView;
    private Spinner accessSpinner, profileSpinner;
    private Button applyButton;
    private final Shizuku.OnRequestPermissionResultListener permissionListener =
        (requestCode, grantResult) -> runOnUiThread(() -> {
            if (requestCode == REQUEST_SHIZUKU) {
                log(grantResult == PackageManager.PERMISSION_GRANTED
                    ? "Izin Shizuku diberikan. Ulangi tindakan yang diperlukan."
                    : "Izin Shizuku ditolak.");
                refreshStatus();
            }
        });

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Shizuku.addRequestPermissionResultListener(permissionListener);
        createUi();
        refreshStatus();
    }

    @Override protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        executor.shutdownNow();
        super.onDestroy();
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String value, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(Color.rgb(231, 237, 243));
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private void space(LinearLayout parent, int height) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private Button makeButton(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        return b;
    }

    private void createUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(22), dp(18), dp(24));
        page.setBackgroundColor(Color.rgb(15, 19, 24));
        scroll.addView(page);
        setContentView(scroll);

        TextView title = label("KIMPUL TWEAKS", 25, true);
        title.setTextColor(Color.rgb(115, 226, 164));
        page.addView(title);
        TextView sub = label("LIGHTWEIGHT EDITION  •  v2.0.0", 12, false);
        sub.setTextColor(Color.rgb(160, 174, 188));
        page.addView(sub);
        space(page, 16);

        statusView = label("Memeriksa perangkat…", 13, false);
        statusView.setPadding(dp(12), dp(12), dp(12), dp(12));
        statusView.setBackgroundColor(Color.rgb(27, 35, 43));
        page.addView(statusView);
        space(page, 14);

        page.addView(label("Metode akses", 15, true));
        accessSpinner = new Spinner(this);
        accessSpinner.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item,
            new String[]{"Otomatis: Root lalu Shizuku", "Root saja", "Shizuku saja"}));
        page.addView(accessSpinner);
        space(page, 10);

        page.addView(label("Profil performa", 15, true));
        profileSpinner = new Spinner(this);
        profileSpinner.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item,
            new String[]{"Seimbang — 1.0x", "Responsif — 0.5x", "Animasi nonaktif — 0x"}));
        page.addView(profileSpinner);
        space(page, 8);

        TextView notice = label(
            "Profil hanya mengatur tiga skala animasi Android. Ini bukan pengubah partisi /system dan tidak menjamin kenaikan FPS. Hasil bergantung pada ROM dan izin perangkat.",
            12, false);
        notice.setTextColor(Color.rgb(245, 202, 120));
        page.addView(notice);
        space(page, 10);

        applyButton = makeButton("Terapkan profil");
        applyButton.setOnClickListener(v -> applyProfile());
        page.addView(applyButton);
        space(page, 7);

        Button backup = makeButton("Backup nilai saat ini");
        backup.setOnClickListener(v -> backupSettings());
        page.addView(backup);
        space(page, 7);

        Button restore = makeButton("Restore backup");
        restore.setOnClickListener(v -> restoreSettings());
        page.addView(restore);
        space(page, 7);

        Button device = makeButton("Perbarui informasi perangkat");
        device.setOnClickListener(v -> refreshStatus());
        page.addView(device);
        space(page, 7);

        Button partitions = makeButton("Periksa partisi (hanya baca)");
        partitions.setOnClickListener(v -> inspectPartitions());
        page.addView(partitions);
        space(page, 14);

        page.addView(label("Log aktivitas", 15, true));
        logView = label("Aplikasi siap.", 12, false);
        logView.setPadding(dp(12), dp(12), dp(12), dp(12));
        logView.setBackgroundColor(Color.rgb(27, 35, 43));
        page.addView(logView);
    }

    private void log(String line) {
        if (logView == null) return;
        logView.append("\n" + line);
    }

    private void replaceLog(String line) {
        if (logView != null) logView.setText(line);
    }

    private boolean rootMayWork() {
        try {
            Process p = new ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start();
            String output = readAll(p.getInputStream());
            int exit = p.waitFor();
            return exit == 0 && output.contains("uid=0");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean shizukuBinderReady() {
        try { return Shizuku.pingBinder(); }
        catch (Throwable t) { return false; }
    }

    private boolean shizukuGranted() {
        try {
            return shizukuBinderReady()
                && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    private void refreshStatus() {
        executor.execute(() -> {
            boolean root = rootMayWork();
            boolean binder = shizukuBinderReady();
            boolean granted = shizukuGranted();
            String info = "Perangkat: " + Build.MANUFACTURER + " " + Build.MODEL
                + "\nAndroid: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"
                + "\nABI: " + Build.SUPPORTED_ABIS[0]
                + "\nRoot: " + (root ? "perintah su berhasil" : "tidak terdeteksi / belum disetujui")
                + "\nShizuku service: " + (binder ? "aktif" : "belum aktif")
                + "\nIzin Shizuku: " + (granted ? "diberikan" : "belum diberikan");
            runOnUiThread(() -> {
                statusView.setText(info);
                replaceLog("Informasi perangkat diperbarui.\nCatatan: status akses dapat berubah sesuai persetujuan pengguna dan kebijakan ROM.");
            });
        });
    }

    private void askShizukuPermission() {
        try {
            if (!Shizuku.pingBinder()) {
                log("Shizuku belum aktif. Mulai layanan dari aplikasi Shizuku terlebih dahulu.");
                return;
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(REQUEST_SHIZUKU);
                log("Permintaan izin Shizuku dikirim. Beri izin lalu ulangi tindakan.");
            }
        } catch (Throwable t) {
            log("Tidak dapat meminta izin Shizuku: " + t.getMessage());
        }
    }

    private int accessMode() { return accessSpinner.getSelectedItemPosition(); }

    private void runShell(String command, Callback callback) {
        int mode = accessMode();
        executor.execute(() -> {
            Result result;
            if (mode == 0 || mode == 1) {
                result = executeRoot(command);
                if (result.ok || mode == 1) {
                    postResult(result, callback);
                    return;
                }
            }
            if (mode == 0 || mode == 2) {
                if (!shizukuGranted()) {
                    runOnUiThread(this::askShizukuPermission);
                    postResult(new Result(false, "Shizuku belum memiliki izin. Beri izin lalu ulangi."), callback);
                    return;
                }
                result = executeShizuku(command);
                postResult(result, callback);
                return;
            }
            postResult(new Result(false, "Metode akses tidak tersedia."), callback);
        });
    }

    private interface Callback { void done(Result result); }
    private static class Result {
        final boolean ok;
        final String output;
        Result(boolean ok, String output) { this.ok = ok; this.output = output == null ? "" : output; }
    }

    private Result executeRoot(String command) {
        try {
            Process p = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
            String out = readAll(p.getInputStream());
            int code = p.waitFor();
            return new Result(code == 0, "Root exit=" + code + (out.isEmpty() ? "" : "\n" + out));
        } catch (Exception e) {
            return new Result(false, "Root gagal: " + e.getMessage());
        }
    }

    private Result executeShizuku(String command) {
        try {
            Process p = Shizuku.newProcess(new String[]{"sh", "-c", command}, null, null);
            String out = readAll(p.getInputStream());
            int code = p.waitFor();
            return new Result(code == 0, "Shizuku exit=" + code + (out.isEmpty() ? "" : "\n" + out));
        } catch (Throwable t) {
            return new Result(false, "Shizuku gagal: " + t.getMessage());
        }
    }

    private static String readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int n;
        while ((n = stream.read(buffer)) != -1) output.write(buffer, 0, n);
        return output.toString("UTF-8").trim();
    }

    private void postResult(Result result, Callback callback) {
        runOnUiThread(() -> {
            log(result.output);
            if (callback != null) callback.done(result);
        });
    }

    private void applyProfile() {
        int selected = profileSpinner.getSelectedItemPosition();
        String value = selected == 0 ? "1.0" : selected == 1 ? "0.5" : "0.0";
        applyButton.setEnabled(false);
        replaceLog("Menerapkan profil animasi " + value + "x…");
        applyNext(0, value, true);
    }

    private void applyNext(int index, String value, boolean allOk) {
        if (index >= KEYS.length) {
            log(allOk ? "Profil selesai diterapkan." : "Profil tidak lengkap. Periksa izin dan log.");
            applyButton.setEnabled(true);
            return;
        }
        runShell("settings put global " + KEYS[index] + " " + value, result ->
            applyNext(index + 1, value, allOk && result.ok));
    }

    private SharedPreferences backupPrefs() {
        return getSharedPreferences("kimpul_settings_backup_v2", MODE_PRIVATE);
    }

    private void backupSettings() {
        replaceLog("Membaca pengaturan untuk backup…");
        readBackupNext(0, new String[KEYS.length], true);
    }

    private void readBackupNext(int index, String[] values, boolean allOk) {
        if (index >= KEYS.length) {
            if (allOk) {
                SharedPreferences.Editor editor = backupPrefs().edit();
                for (int i = 0; i < KEYS.length; i++) editor.putString(KEYS[i], values[i]);
                editor.putBoolean("backup_exists", true).apply();
                log("Backup tersimpan di penyimpanan privat aplikasi.");
            } else {
                log("Backup gagal/tidak lengkap; tidak menyimpan snapshot parsial.");
            }
            return;
        }
        runShell("settings get global " + KEYS[index], result -> {
            String value = result.output;
            int marker = value.indexOf("\n");
            if (marker >= 0) value = value.substring(marker + 1).trim();
            else if (value.startsWith("Root exit=") || value.startsWith("Shizuku exit=")) value = "";
            if (!result.ok) {
                log("Tidak dapat membaca " + KEYS[index]);
                readBackupNext(index + 1, values, false);
                return;
            }
            if (value.equals("null") || value.isEmpty()) value = BACKUP_NONE;
            if (!value.equals(BACKUP_NONE) && !value.matches("[0-9]+(\\.[0-9]+)?")) {
                log("Nilai tidak dikenali untuk " + KEYS[index] + ": " + value);
                readBackupNext(index + 1, values, false);
                return;
            }
            values[index] = value;
            log(KEYS[index] + " = " + (value.equals(BACKUP_NONE) ? "(belum disetel)" : value));
            readBackupNext(index + 1, values, allOk);
        });
    }

    private void restoreSettings() {
        SharedPreferences p = backupPrefs();
        if (!p.getBoolean("backup_exists", false)) {
            log("Belum ada backup. Jalankan Backup nilai saat ini terlebih dahulu.");
            return;
        }
        replaceLog("Memulihkan backup…");
        restoreNext(0, true);
    }

    private void restoreNext(int index, boolean allOk) {
        if (index >= KEYS.length) {
            log(allOk ? "Restore selesai." : "Restore tidak lengkap. Periksa log dan izin.");
            return;
        }
        String value = backupPrefs().getString(KEYS[index], null);
        if (value == null) {
            log("Backup tidak lengkap untuk " + KEYS[index]);
            restoreNext(index + 1, false);
            return;
        }
        String command;
        if (value.equals(BACKUP_NONE)) command = "settings delete global " + KEYS[index];
        else if (value.matches("[0-9]+(\\.[0-9]+)?")) command = "settings put global " + KEYS[index] + " " + value;
        else {
            log("Nilai backup tidak valid untuk " + KEYS[index]);
            restoreNext(index + 1, false);
            return;
        }
        runShell(command, result -> restoreNext(index + 1, allOk && result.ok));
    }

    private void inspectPartitions() {
        replaceLog("Memeriksa mount sistem dalam mode baca saja…");
        runShell("cat /proc/mounts | grep -E ' /(system|vendor|product|odm|system_ext)( |/)'", result -> {
            if (result.output.trim().isEmpty()) {
                log("Tidak ada baris mount yang cocok, atau perintah ditolak. Ini tidak membuktikan partisi tidak ada.");
            }
            log("Pemeriksaan hanya membaca /proc/mounts. Aplikasi tidak mencoba remount atau menulis partisi.");
        });
    }
}
