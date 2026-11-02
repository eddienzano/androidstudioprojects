
package com.aaagrowers.coldroom.stocks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aaagrowers.coldroom.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GradedStockScanActivity extends Activity {

    private static final long DOUBLE_SCAN_LOCK_MS = 2000;
    private static final long HARDWARE_SCAN_PAUSE_MS = 250;

    private EditText input;

    private Button btnCamera;
    private Button btnSend;
    private Button btnReset;

    private TextView tvRecent;
    private TextView tvTotal;
    private TextView tvValid;
    private TextView tvDuplicate;
    private TextView tvNotFound;

    private int farmId;

    private int totalCount = 0;
    private int validCount = 0;
    private int duplicateCount = 0;
    private int notFoundCount = 0;

    private final Set<String> scannedSet = new HashSet<>();
    private final Deque<String> recentDeque = new ArrayDeque<>();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable hardwareScanRunnable = () -> {
        String qr = input.getText().toString().trim();
        input.setText("");

        if (!qr.isEmpty()) {
            submitIfAllowed(qr);
        }
    };

    private final ToneGenerator tone = new ToneGenerator(
            AudioManager.STREAM_NOTIFICATION,
            80
    );

    private Vibrator vibrator;

    private long lastScannedAt = 0L;
    private String lastScannedValue = "";

    private boolean confirmationShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_graded_stock_scan);

        farmId = getIntent().getIntExtra("farm", 1);

        input = findViewById(R.id.inputScan);
        btnCamera = findViewById(R.id.btnCameraScan);
        btnSend = findViewById(R.id.btnSendDb);
        btnReset = findViewById(R.id.btnReset);

        tvRecent = findViewById(R.id.tvRecent);
        tvTotal = findViewById(R.id.tvTotal);
        tvValid = findViewById(R.id.tvValid);
        tvDuplicate = findViewById(R.id.tvDuplicate);
        tvNotFound = findViewById(R.id.tvNotFound);

        vibrator = (Vibrator) getSystemService(
                Context.VIBRATOR_SERVICE
        );

        /*
         * Reset the visible counters for this scanning session.
         * This does not delete saved offline scans.
         */
        btnReset.setOnClickListener(v -> {
            totalCount = 0;
            validCount = 0;
            duplicateCount = 0;
            notFoundCount = 0;

            scannedSet.clear();
            recentDeque.clear();

            tvRecent.setText("");

            lastScannedValue = "";
            lastScannedAt = 0L;

            updateStats();
        });

        /*
         * Upload corrections first so the server inventory is corrected
         * before ordinary scan records are created from that inventory.
         */
        btnSend.setOnClickListener(v -> {
            btnSend.setEnabled(false);
            showToast("Syncing inventory fixes and scans...");

            new Thread(() -> {
                boolean fixesOk = GradedStockRepository.uploadInventoryFixes(
                        getApplicationContext(), farmId);

                // Do not upload scans if fixes failed; otherwise the server could
                // record stale variety/length values for a corrected QR.
                boolean scansOk = fixesOk && GradedStockRepository.uploadScans(
                        getApplicationContext(), farmId);

                runOnUiThread(() -> {
                    btnSend.setEnabled(true);
                    if (!fixesOk) {
                        showToast("Some inventory fixes remain pending. " +
                                "Scans were not uploaded to avoid stale details.");
                    } else if (!scansOk) {
                        showToast("Fixes synced, but some scans remain pending.");
                    } else {
                        showToast("Inventory fixes and scans synced successfully.");
                    }
                });
            }).start();
        });

        /*
         * Camera scanner.
         */
        btnCamera.setOnClickListener(v -> {
            if (confirmationShowing) {
                showToast("Finish the current scan first.");
                return;
            }

            IntentIntegrator integrator =
                    new IntentIntegrator(this);

            integrator.setPrompt("Scan Graded Stock QR Code");
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });

        /*
         * Hardware scanner.
         *
         * Wait briefly after the last character so a scanner that types
         * a QR value character by character does not submit just its
         * first character.
         */
        input.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mainHandler.removeCallbacks(hardwareScanRunnable);

                if (confirmationShowing) {
                    return;
                }

                if (s != null && s.length() > 0) {
                    mainHandler.postDelayed(
                            hardwareScanRunnable,
                            HARDWARE_SCAN_PAUSE_MS
                    );
                }
            }
        });

        updateStats();
    }

    // =========================================================
    // SCAN CONTROL
    // =========================================================

    private void submitIfAllowed(String qr) {
        if (qr == null || qr.trim().isEmpty()) {
            return;
        }

        qr = qr.trim();

        long now = System.currentTimeMillis();

        if (qr.equals(lastScannedValue)
                && (now - lastScannedAt) < DOUBLE_SCAN_LOCK_MS) {
            return;
        }

        lastScannedValue = qr;
        lastScannedAt = now;

        doScan(qr);
    }

    // =========================================================
    // MAIN SCAN
    // =========================================================

    private void doScan(String qr) {
        totalCount++;
        updateStats();

        /*
         * A duplicate is a QR already confirmed during this session.
         */
        if (scannedSet.contains(qr)) {
            duplicateCount++;
            updateStats();
            vibrate();
            addRecent(qr, "DUPLICATE ⚠");
            return;
        }

        /*
         * Look up the QR before saving anything.
         * Unknown QR codes must not be saved as confirmed scans.
         */
        JSONObject inventory =
                GradedStockRepository.getGradedStock(this, qr);

        if (inventory == null) {
            notFoundCount++;
            updateStats();
            vibrate();

            addRecent(qr, "NOT FOUND — NOT RECORDED");

            new AlertDialog.Builder(this)
                    .setTitle("QR Not Found")
                    .setMessage(
                            "This QR code was not found in the local "
                                    + "graded-stock database.\n\n"
                                    + "QR: " + qr
                                    + "\n\nNo scan has been recorded. "
                                    + "Synchronize graded stock and try again."
                    )
                    .setPositiveButton("OK", (dialog, which) -> {
                        confirmationShowing = false;
                    })
                    .setOnCancelListener(dialog -> {
                        confirmationShowing = false;
                    })
                    .show();

            confirmationShowing = true;
            return;
        }

        showRecordConfirmation(qr, inventory);
    }

    // =========================================================
    // CONFIRMATION DIALOG
    // =========================================================

    private void showRecordConfirmation(String qr, JSONObject inventory) {
        String variety = inventory.optString("variety", "Unknown");
        int varietyId = inventory.optInt("variety_id", 0);
        int length = inventory.optInt("length", 0);
        int bunches = inventory.optInt("remaining_bunches", 0);
        int stems = inventory.optInt("stems_per_bunch", 0);

        LinearLayout content = dialogColumn();
        content.addView(dialogEyebrow("GRADED STOCK • SCAN REVIEW"));
        content.addView(dialogHeading("Confirm stock details"));
        content.addView(dialogBody("Check the details below before recording this QR."));

        LinearLayout qrCard = dialogCard();
        qrCard.addView(dialogLabel("QR CODE / SERIAL"));
        TextView qrValue = dialogText(extractSerial(qr), 18, true, DARK);
        qrValue.setTextIsSelectable(true);
        qrCard.addView(qrValue);
        TextView fullQr = dialogText(qr, 12, false, MUTED);
        fullQr.setTextIsSelectable(true);
        LinearLayout.LayoutParams fullQrParams = matchWrap();
        fullQrParams.topMargin = dp(5);
        qrCard.addView(fullQr, fullQrParams);
        content.addView(qrCard, matchWrap());

        LinearLayout details = dialogCard();
        details.addView(dialogLabel("INVENTORY DETAILS"));
        addDetailRow(details, "Variety", variety);
        addDetailRow(details, "Variety ID", String.valueOf(varietyId));
        addDetailRow(details, "Length", length + " cm");
        addDetailRow(details, "Remaining bunches", String.valueOf(bunches));
        addDetailRow(details, "Stems per bunch", String.valueOf(stems));
        content.addView(details, matchWrap());

        confirmationShowing = true;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setPositiveButton("Record scan", null)
                .setNeutralButton("Fix details", null)
                .setNegativeButton("Cancel", (d, which) ->
                        addRecent(qr, "CANCELLED — NOT RECORDED"))
                .create();

        dialog.setOnCancelListener(d -> confirmationShowing = false);
        dialog.setOnDismissListener(d -> confirmationShowing = false);
        dialog.setOnShowListener(d -> {
            styleDialogButtons(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                confirmationShowing = false;
                recordConfirmedScan(qr, inventory);
                dialog.dismiss();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                dialog.dismiss();
                showFixGradedStockDialog(qr, inventory);
            });
        });
        dialog.show();
    }

    // =========================================================
    // RECORD CONFIRMED SCAN
    // =========================================================

    private void recordConfirmedScan(
            String qr,
            JSONObject inventory
    ) {
        if (scannedSet.contains(qr)) {
            duplicateCount++;
            updateStats();
            vibrate();
            addRecent(qr, "DUPLICATE ⚠");
            return;
        }

        /*
         * Mark the QR as scanned and save it offline only after
         * the user has chosen Record or Save & Record.
         */
        scannedSet.add(qr);

        GradedStockRepository.saveScan(
                getApplicationContext(),
                qr,
                farmId
        );

        validCount++;
        updateStats();

        String variety = inventory.optString("variety", "Unknown");
        int length = inventory.optInt("length", 0);
        int bunches = inventory.optInt("remaining_bunches", 0);
        int stems = inventory.optInt("stems_per_bunch", 0);

        String status = variety + " ✓";

        if (length > 0) {
            status += " • " + length + "cm";
        }

        status += "\n" + bunches + " bunches • "
                + stems + " stems/bunch";

        addRecent(qr, status);
        beepSuccess();
    }

    // =========================================================
    // FIX STOCK DETAILS
    // =========================================================

    private void showFixGradedStockDialog(String qr, JSONObject inventory) {
        List<VarietyOption> varieties = loadVarieties();
        if (varieties.isEmpty()) {
            confirmationShowing = false;
            new AlertDialog.Builder(this)
                    .setTitle("No varieties available")
                    .setMessage("No varieties were found in variety.db. Synchronize or import the variety list before correcting this stock.")
                    .setPositiveButton("OK", null)
                    .show();
            addRecent(qr, "FIX FAILED — NO VARIETIES");
            return;
        }

        LinearLayout content = dialogColumn();
        content.addView(dialogEyebrow("GRADED STOCK • CORRECTION"));
        content.addView(dialogHeading("Correct stock details"));
        content.addView(dialogBody("Update the values for this QR. The correction is saved offline and sent when you sync."));

        LinearLayout qrCard = dialogCard();
        qrCard.addView(dialogLabel("QR CODE / SERIAL"));
        TextView qrValue = dialogText(extractSerial(qr), 17, true, DARK);
        qrValue.setTextIsSelectable(true);
        qrCard.addView(qrValue);
        TextView fullQr = dialogText(qr, 12, false, MUTED);
        fullQr.setTextIsSelectable(true);
        LinearLayout.LayoutParams qrParams = matchWrap();
        qrParams.topMargin = dp(4);
        qrCard.addView(fullQr, qrParams);
        content.addView(qrCard, matchWrap());

        LinearLayout formCard = dialogCard();
        formCard.addView(dialogLabel("CORRECTED INVENTORY"));
        TextView varietyLabel = dialogText("Variety", 14, true, DARK);
        LinearLayout.LayoutParams fieldLabelParams = matchWrap();
        fieldLabelParams.topMargin = dp(12);
        formCard.addView(varietyLabel, fieldLabelParams);

        Spinner varietySpinner = new Spinner(this);
        ArrayAdapter<VarietyOption> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, varieties);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        varietySpinner.setAdapter(adapter);
        varietySpinner.setPadding(dp(8), dp(5), dp(8), dp(5));
        varietySpinner.setBackground(roundRect(Color.WHITE, BORDER, 10));
        int currentVarietyId = inventory.optInt("variety_id", -1);
        for (int i = 0; i < varieties.size(); i++) {
            if (varieties.get(i).id == currentVarietyId) {
                varietySpinner.setSelection(i);
                break;
            }
        }
        LinearLayout.LayoutParams spinnerParams = matchWrap();
        spinnerParams.topMargin = dp(6);
        formCard.addView(varietySpinner, spinnerParams);

        EditText lengthInput = createNumberInput("Length in centimetres", inventory.optInt("length", 0));
        EditText bunchesInput = createNumberInput("Remaining bunches", inventory.optInt("remaining_bunches", 0));
        EditText stemsInput = createNumberInput("Stems per bunch", inventory.optInt("stems_per_bunch", 0));
        addField(formCard, "Length (cm)", lengthInput);
        addField(formCard, "Remaining bunches", bunchesInput);
        addField(formCard, "Stems per bunch", stemsInput);
        content.addView(formCard, matchWrap());

        TextView footer = dialogBody("Changes stay on this device until synchronization succeeds.");
        footer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footerParams = matchWrap();
        footerParams.topMargin = dp(2);
        content.addView(footer, footerParams);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setClipToPadding(false);
        scrollView.setPadding(0, 0, 0, dp(4));
        scrollView.addView(content);

        confirmationShowing = true;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scrollView)
                .setPositiveButton("Save & record", null)
                .setNegativeButton("Cancel", (d, which) ->
                        addRecent(qr, "FIX CANCELLED — NOT RECORDED"))
                .create();

        dialog.setOnCancelListener(d -> confirmationShowing = false);
        dialog.setOnDismissListener(d -> confirmationShowing = false);
        dialog.setOnShowListener(d -> {
            styleDialogButtons(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                VarietyOption selected = (VarietyOption) varietySpinner.getSelectedItem();
                if (selected == null) {
                    showToast("Select a variety.");
                    return;
                }
                Integer length = parsePositiveOrZero(lengthInput.getText().toString());
                Integer bunches = parsePositiveOrZero(bunchesInput.getText().toString());
                Integer stems = parsePositiveOrZero(stemsInput.getText().toString());
                if (length == null || bunches == null || stems == null
                        || length <= 0 || bunches < 0 || stems <= 0) {
                    showToast("Enter a valid length, bunch count and stems per bunch.");
                    return;
                }

                boolean updated = GradedStockRepository.updateGradedStock(
                        getApplicationContext(), qr, farmId, selected.id,
                        selected.name, length, bunches, stems);
                if (!updated) {
                    showToast("Could not update the local stock record.");
                    return;
                }

                JSONObject corrected = GradedStockRepository.getGradedStock(
                        getApplicationContext(), qr);
                if (corrected == null) {
                    showToast("Updated stock could not be reloaded.");
                    return;
                }
                dialog.dismiss();
                recordConfirmedScan(qr, corrected);
                addRecent(qr, "CORRECTED • " + selected.name + " (ID: " + selected.id + ")");
            });
        });
        dialog.show();
    }

    // =========================================================
    // VARIETY DATABASE
    // =========================================================

    private static class VarietyOption {
        final int id;
        final String name;

        VarietyOption(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name + " (ID: " + id + ")";
        }
    }

    private List<VarietyOption> loadVarieties() {
        List<VarietyOption> varieties = new ArrayList<>();

        VarietyDbHelper helper = null;
        android.database.sqlite.SQLiteDatabase db = null;
        android.database.Cursor cursor = null;

        try {
            helper = new VarietyDbHelper(this);
            db = helper.getReadableDatabase();

            cursor = db.rawQuery(
                    "SELECT variety_id, variety_name "
                            + "FROM variety "
                            + "ORDER BY variety_name COLLATE NOCASE ASC",
                    null
            );

            while (cursor.moveToNext()) {
                int id = cursor.getInt(
                        cursor.getColumnIndexOrThrow("variety_id")
                );

                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("variety_name")
                );

                if (name != null && !name.trim().isEmpty()) {
                    varieties.add(
                            new VarietyOption(id, name.trim())
                    );
                }
            }

        } catch (Exception e) {
            android.util.Log.e(
                    "GradedStockScan",
                    "Could not load varieties from variety.db",
                    e
            );

        } finally {
            if (cursor != null) {
                cursor.close();
            }

            if (db != null && db.isOpen()) {
                db.close();
            }

            if (helper != null) {
                helper.close();
            }
        }

        return varieties;
    }

    // =========================================================
    // INPUT HELPERS
    // =========================================================

    private static final int DARK = Color.rgb(31, 41, 55);
    private static final int MUTED = Color.rgb(107, 114, 128);
    private static final int BORDER = Color.rgb(226, 232, 240);
    private static final int SURFACE = Color.rgb(248, 250, 252);
    private static final int ACCENT = Color.rgb(22, 101, 86);

    private LinearLayout dialogColumn() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(20), dp(18), dp(20), dp(10));
        column.setBackgroundColor(Color.WHITE);
        return column;
    }

    private LinearLayout dialogCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(roundRect(SURFACE, BORDER, 14));
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(10);
        card.setLayoutParams(params);
        return card;
    }

    private TextView dialogEyebrow(String text) {
        TextView view = dialogText(text, 11, true, ACCENT);
        view.setLetterSpacing(0.08f);
        return view;
    }

    private TextView dialogHeading(String text) {
        TextView view = dialogText(text, 22, true, DARK);
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(5);
        view.setLayoutParams(params);
        return view;
    }

    private TextView dialogBody(String text) {
        TextView view = dialogText(text, 13, false, MUTED);
        view.setLineSpacing(dp(2), 1.0f);
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(4);
        view.setLayoutParams(params);
        return view;
    }

    private TextView dialogLabel(String text) {
        TextView view = dialogText(text, 11, true, MUTED);
        view.setLetterSpacing(0.05f);
        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(7);
        view.setLayoutParams(params);
        return view;
    }

    private TextView dialogText(String text, int sizeSp, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private void addDetailRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView left = dialogText(label, 13, false, MUTED);
        TextView right = dialogText(value, 14, true, DARK);
        right.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(7);
        parent.addView(row, params);
    }

    private void addField(LinearLayout parent, String label, EditText input) {
        TextView labelView = dialogText(label, 13, true, DARK);
        LinearLayout.LayoutParams labelParams = matchWrap();
        labelParams.topMargin = dp(12);
        parent.addView(labelView, labelParams);
        LinearLayout.LayoutParams inputParams = matchWrap();
        inputParams.topMargin = dp(5);
        parent.addView(input, inputParams);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private GradientDrawable roundRect(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private void styleDialogButtons(AlertDialog dialog) {
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (positive != null) {
            positive.setTextColor(ACCENT);
            positive.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        if (neutral != null) {
            neutral.setTextColor(ACCENT);
            neutral.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        if (negative != null) negative.setTextColor(MUTED);
    }

    private EditText createNumberInput(
            String hint,
            int value
    ) {
        EditText editText = new EditText(this);

        editText.setHint(hint);
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setText(String.valueOf(value));
        editText.setTextSize(16);
        editText.setPadding(dp(12), dp(11), dp(12), dp(11));
        editText.setSelectAllOnFocus(true);
        editText.setBackground(roundRect(Color.WHITE, BORDER, 10));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.topMargin = dp(8);
        editText.setLayoutParams(params);

        return editText;
    }

    private Integer parsePositiveOrZero(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }

            int parsed = Integer.parseInt(value.trim());

            if (parsed < 0) {
                return null;
            }

            return parsed;

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    // =========================================================
    // RECENT SCANS
    // =========================================================

    private void addRecent(String qr, String status) {
        mainHandler.post(() -> {
            if (recentDeque.size() >= 10) {
                recentDeque.removeLast();
            }

            String displayQr = extractSerial(qr);
            String line = displayQr + "\n" + status;

            recentDeque.addFirst(line);

            StringBuilder sb = new StringBuilder();

            for (String recent : recentDeque) {
                sb.append(recent).append("\n\n");
            }

            tvRecent.setText(sb.toString().trim());
        });
    }

    private String extractSerial(String qr) {
        if (qr == null) {
            return "";
        }

        String[] parts = qr.split("\\|");

        for (String part : parts) {
            if (part.startsWith("Serial:")) {
                return part.substring("Serial:".length()).trim();
            }
        }

        return qr.trim();
    }

    // =========================================================
    // UPDATE COUNTERS
    // =========================================================

    private void updateStats() {
        runOnUiThread(() -> {
            tvTotal.setText(String.valueOf(totalCount));
            tvValid.setText(String.valueOf(validCount));
            tvDuplicate.setText(String.valueOf(duplicateCount));
            tvNotFound.setText(String.valueOf(notFoundCount));
        });
    }

    // =========================================================
    // SUCCESS BEEP
    // =========================================================

    private void beepSuccess() {
        try {
            tone.startTone(
                    ToneGenerator.TONE_PROP_BEEP,
                    100
            );
        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // VIBRATION
    // =========================================================

    private void vibrate() {
        try {
            if (vibrator != null) {
                vibrator.vibrate(200);
            }
        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // CAMERA RESULT
    // =========================================================

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        IntentResult result =
                IntentIntegrator.parseActivityResult(
                        requestCode,
                        resultCode,
                        data
                );

        if (result != null) {
            String content = result.getContents();

            if (content != null) {
                submitIfAllowed(content.trim());
            } else {
                showToast("Scan cancelled");
            }

        } else {
            super.onActivityResult(
                    requestCode,
                    resultCode,
                    data
            );
        }
    }

    // =========================================================
    // TOAST
    // =========================================================

    private void showToast(String message) {
        mainHandler.post(() -> {
            Toast toast = Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_SHORT
            );

            toast.setGravity(
                    Gravity.CENTER,
                    0,
                    0
            );

            toast.show();
        });
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(hardwareScanRunnable);

        try {
            tone.release();
        } catch (Exception ignored) {
        }

        super.onDestroy();
    }
}
