
package com.aaagrowers.coldroom;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseBackupActivity extends AppCompatActivity {

    private static final String TAG = "DatabaseBackupActivity";

    // Replace this with the team's actual email address.
    private static final String TEAM_EMAIL = "rosesdata@aaagrowers.co.ke";

    private static final int NAVY = Color.rgb(18, 37, 64);
    private static final int BLUE = Color.rgb(37, 99, 235);
    private static final int GREEN = Color.rgb(22, 163, 74);
    private static final int BACKGROUND = Color.rgb(246, 248, 252);
    private static final int TEXT = Color.rgb(31, 41, 55);
    private static final int MUTED = Color.rgb(107, 114, 128);
    private static final int BORDER = Color.rgb(229, 234, 242);
    private static final int WHITE = Color.WHITE;

    private TextView statusText;
    private TextView fileNameText;
    private TextView fileSizeText;
    private TextView backupDateText;
    private TextView backupStatusText;

    private Button createButton;
    private Button exportButton;
    private Button emailButton;

    private File latestBackup;

    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument(
                            "application/zip"
                    ),
                    this::exportBackup
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );

        buildInterface();
        showLatestBackup();
    }

    private void buildInterface() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BACKGROUND);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);

        scrollView.addView(page);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(24), dp(28), dp(24), dp(30));
        header.setBackground(background(NAVY, 0));

        TextView brand = new TextView(this);
        brand.setText("AAA GROWERS  /  COLDROOM");
        brand.setTextColor(Color.rgb(174, 199, 239));
        brand.setTextSize(12);
        brand.setTypeface(null, Typeface.BOLD);
        brand.setLetterSpacing(0.08f);
        header.addView(brand);

        TextView heading = new TextView(this);
        heading.setText("Database Backup");
        heading.setTextColor(WHITE);
        heading.setTextSize(28);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setPadding(0, dp(15), 0, dp(7));
        header.addView(heading);

        TextView subtitle = new TextView(this);
        subtitle.setText(
                "Protect your coldroom records and keep a copy " +
                        "ready for recovery or transfer."
        );
        subtitle.setTextColor(Color.rgb(220, 228, 241));
        subtitle.setTextSize(14);
        subtitle.setLineSpacing(dp(4), 1f);
        header.addView(subtitle);

        page.addView(header);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(22), dp(18), dp(28));
        page.addView(content);

        // Latest backup card
        addSectionTitle(content, "LATEST BACKUP");

        LinearLayout backupCard = card();
        backupCard.setOrientation(LinearLayout.VERTICAL);
        backupCard.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = new TextView(this);
        icon.setText("✓");
        icon.setTextSize(20);
        icon.setTextColor(GREEN);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(background(
                Color.rgb(225, 247, 233), 50
        ));
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(44), dp(44));
        iconParams.setMarginEnd(dp(12));
        statusRow.addView(icon, iconParams);

        LinearLayout statusColumn = new LinearLayout(this);
        statusColumn.setOrientation(LinearLayout.VERTICAL);

        TextView cardTitle = new TextView(this);
        cardTitle.setText("Backup information");
        cardTitle.setTextColor(TEXT);
        cardTitle.setTextSize(16);
        cardTitle.setTypeface(null, Typeface.BOLD);
        statusColumn.addView(cardTitle);

        backupStatusText = new TextView(this);
        backupStatusText.setText("Checking backup status...");
        backupStatusText.setTextColor(MUTED);
        backupStatusText.setTextSize(12);
        backupStatusText.setPadding(0, dp(4), 0, 0);
        statusColumn.addView(backupStatusText);

        statusRow.addView(statusColumn,
                new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
                ));

        backupCard.addView(statusRow);

        addDivider(backupCard);

        addInfoRow(backupCard, "FILE NAME", "No backup available", true);
        fileNameText = lastInfoValue;

        addInfoRow(backupCard, "FILE SIZE", "—", false);
        fileSizeText = lastInfoValue;

        addInfoRow(backupCard, "BACKUP DATE", "—", false);
        backupDateText = lastInfoValue;

        content.addView(backupCard);

        // Create action
        addSectionTitle(content, "BACKUP ACTIONS");

        TextView actionDescription = new TextView(this);
        actionDescription.setText(
                "Create a ZIP restore point, save a copy outside " +
                        "the app, or email it to the team."
        );
        actionDescription.setTextSize(13);
        actionDescription.setTextColor(MUTED);
        actionDescription.setPadding(0, 0, 0, dp(14));
        content.addView(actionDescription);

        createButton = new Button(this);
        createButton.setText("＋   Create New Backup");
        createButton.setTextColor(WHITE);
        createButton.setTextSize(15);
        createButton.setAllCaps(false);
        createButton.setTypeface(null, Typeface.BOLD);
        createButton.setBackground(background(BLUE, 14));
        createButton.setElevation(dp(2));

        content.addView(createButton, buttonParams(54));

        createButton.setOnClickListener(v -> createBackup());

        exportButton = new Button(this);
        exportButton.setText("↓   Export Backup to Device");
        exportButton.setTextColor(NAVY);
        exportButton.setTextSize(14);
        exportButton.setAllCaps(false);
        exportButton.setBackground(background(WHITE, 14));
        exportButton.setElevation(0);

        LinearLayout.LayoutParams exportParams = buttonParams(52);
        exportParams.topMargin = dp(10);
        content.addView(exportButton, exportParams);

        exportButton.setOnClickListener(v -> exportLatestBackup());

        emailButton = new Button(this);
        emailButton.setText("✉   Email Backup to Team");
        emailButton.setTextColor(WHITE);
        emailButton.setTextSize(14);
        emailButton.setAllCaps(false);
        emailButton.setTypeface(null, Typeface.BOLD);
        emailButton.setBackground(background(GREEN, 14));

        LinearLayout.LayoutParams emailParams = buttonParams(52);
        emailParams.topMargin = dp(10);
        content.addView(emailButton, emailParams);

        emailButton.setOnClickListener(v -> emailLatestBackup());

        // Status panel
        LinearLayout statusCard = card();
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(dp(16), dp(15), dp(16), dp(15));

        LinearLayout.LayoutParams statusCardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        statusCardParams.topMargin = dp(22);
        content.addView(statusCard, statusCardParams);

        TextView activityHeading = new TextView(this);
        activityHeading.setText("ACTIVITY STATUS");
        activityHeading.setTextSize(11);
        activityHeading.setTypeface(null, Typeface.BOLD);
        activityHeading.setTextColor(MUTED);
        statusCard.addView(activityHeading);

        statusText = new TextView(this);
        statusText.setText("Ready to create a backup.");
        statusText.setTextColor(TEXT);
        statusText.setTextSize(13);
        statusText.setPadding(0, dp(8), 0, 0);
        statusText.setLineSpacing(dp(3), 1f);
        statusCard.addView(statusText);

        TextView footer = new TextView(this);
        footer.setText(
                "Keep exported backups in a secure location. " +
                        "Email availability and attachment-size limits depend " +
                        "on the selected email provider."
        );
        footer.setTextSize(11);
        footer.setTextColor(MUTED);
        footer.setLineSpacing(dp(3), 1f);
        footer.setPadding(dp(2), dp(18), dp(2), 0);
        content.addView(footer);

        setContentView(scrollView);
    }

    private TextView lastInfoValue;

    private void addInfoRow(
            LinearLayout parent,
            String label,
            String value,
            boolean first
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(13), 0, dp(11));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(10);
        labelView.setTypeface(null, Typeface.BOLD);
        labelView.setTextColor(MUTED);
        row.addView(labelView);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(13);
        valueView.setTextColor(TEXT);
        valueView.setTypeface(null, Typeface.BOLD);
        valueView.setPadding(0, dp(5), 0, 0);
        valueView.setTextIsSelectable(true);
        row.addView(valueView);

        parent.addView(row);
        lastInfoValue = valueView;
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(BORDER);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
                );
        params.topMargin = dp(16);
        parent.addView(divider, params);
    }

    private void addSectionTitle(LinearLayout parent, String title) {
        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(MUTED);
        label.setTextSize(11);
        label.setTypeface(null, Typeface.BOLD);
        label.setLetterSpacing(0.08f);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        params.bottomMargin = dp(12);
        parent.addView(label, params);
    }

    private LinearLayout card() {
        LinearLayout result = new LinearLayout(this);
        result.setBackground(background(WHITE, 18));
        result.setElevation(dp(1));
        return result;
    }

    private LinearLayout.LayoutParams buttonParams(int heightDp) {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(heightDp)
        );
    }

    private GradientDrawable background(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));

        if (color == WHITE) {
            drawable.setStroke(dp(1), BORDER);
        }

        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private void createBackup() {
        createButton.setEnabled(false);
        statusText.setText("Creating and validating the backup...");

        try {
            latestBackup = DatabaseBackupUtils.createBackup(
                    this,
                    "coldroom_"
            );

            if (latestBackup == null || !latestBackup.isFile()
                    || latestBackup.length() == 0) {
                throw new Exception("The backup file was not created correctly.");
            }

            updateBackupDetails();

            statusText.setText(
                    "Backup created successfully.\n\n" +
                            "Your ZIP backup is ready to export or email."
            );

            Toast.makeText(
                    this,
                    "Database backup created successfully.",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {
            statusText.setText("Backup failed: " + safeMessage(e));

            Toast.makeText(
                    this,
                    "Backup failed. See the activity status.",
                    Toast.LENGTH_LONG
            ).show();

        } finally {
            createButton.setEnabled(true);
        }
    }

    private void showLatestBackup() {
        List<File> backups = DatabaseBackupUtils.getBackups(this);

        if (backups == null || backups.isEmpty()) {
            latestBackup = null;
            updateBackupDetails();
            statusText.setText("No local backups found. Create your first backup.");
            return;
        }

        // getBackups() should return newest first.
        latestBackup = backups.get(0);
        updateBackupDetails();
        statusText.setText("Latest local backup is ready.");
    }

    private void updateBackupDetails() {
        boolean available = latestBackup != null
                && latestBackup.isFile()
                && latestBackup.length() > 0;

        if (!available) {
            fileNameText.setText("No backup available");
            fileSizeText.setText("—");
            backupDateText.setText("—");
            backupStatusText.setText("Create a backup to get started.");

            exportButton.setEnabled(false);
            emailButton.setEnabled(false);
            return;
        }

        fileNameText.setText(latestBackup.getName());
        fileSizeText.setText(formatBytes(latestBackup.length()));

        String date = new SimpleDateFormat(
                "dd MMM yyyy, HH:mm",
                Locale.getDefault()
        ).format(new Date(latestBackup.lastModified()));

        backupDateText.setText(date);
        backupStatusText.setText("Local backup available");

        exportButton.setEnabled(true);
        emailButton.setEnabled(true);
    }

    private boolean hasUsableBackup() {
        if (latestBackup == null
                || !latestBackup.isFile()
                || latestBackup.length() == 0) {
            showLatestBackup();
        }

        if (latestBackup == null
                || !latestBackup.isFile()
                || latestBackup.length() == 0) {
            Toast.makeText(
                    this,
                    "Create a backup before continuing.",
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }

        return true;
    }

    private void exportLatestBackup() {
        if (!hasUsableBackup()) {
            return;
        }

        exportLauncher.launch(latestBackup.getName());
    }

    private void exportBackup(Uri destinationUri) {
        if (destinationUri == null) {
            statusText.setText("Export cancelled.");
            return;
        }

        if (!hasUsableBackup()) {
            return;
        }

        try (
                InputStream input = new FileInputStream(latestBackup);
                OutputStream output =
                        getContentResolver().openOutputStream(destinationUri, "wt")
        ) {
            if (output == null) {
                throw new java.io.IOException(
                        "Could not open the selected destination."
                );
            }

            byte[] buffer = new byte[8192];
            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }

            output.flush();

            statusText.setText(
                    "Backup exported successfully.\n\n" +
                            "Saved as: " + latestBackup.getName()
            );

            Toast.makeText(
                    this,
                    "Backup exported successfully.",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {
            statusText.setText("Export failed: " + safeMessage(e));

            Toast.makeText(
                    this,
                    "Could not export the backup.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    private void emailLatestBackup() {
        if (!hasUsableBackup()) {
            return;
        }

        try {
            // Confirm the file can actually be read before sharing it.
            if (!latestBackup.canRead()) {
                throw new java.io.IOException(
                        "The backup file cannot be read."
                );
            }

            Uri attachmentUri = FileProvider.getUriForFile(
                    getApplicationContext(),
                    getPackageName() + ".fileprovider",
                    latestBackup
            );

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("application/zip");

            sendIntent.putExtra(
                    Intent.EXTRA_EMAIL,
                    new String[]{"rosesdata@aaagrowers.co.ke"}
            );

            sendIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Coldroom Database Backup - " +
                            new SimpleDateFormat(
                                    "dd MMM yyyy HH:mm",
                                    Locale.getDefault()
                            ).format(new Date())
            );

            sendIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Hello Team,\n\n" +
                            "Please find attached the coldroom database backup.\n\n" +
                            "File: " + latestBackup.getName() + "\n" +
                            "Size: " + formatBytes(latestBackup.length()) + "\n\n" +
                            "Regards,\nAAA Growers"
            );

            sendIntent.putExtra(Intent.EXTRA_STREAM, attachmentUri);

            // Explicitly attach the URI to the intent's ClipData as well.
            sendIntent.setClipData(
                    android.content.ClipData.newUri(
                            getContentResolver(),
                            latestBackup.getName(),
                            attachmentUri
                    )
            );

            sendIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            Intent chooser = Intent.createChooser(
                    sendIntent,
                    "Send Coldroom Database Backup"
            );

            startActivity(chooser);

            statusText.setText(
                    "Email application opened.\n\n" +
                            "Confirm the attachment and recipient, then send."
            );

        } catch (ActivityNotFoundException e) {
            statusText.setText("No compatible email application was found.");

            Toast.makeText(
                    this,
                    "No compatible email application was found.",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {
            statusText.setText(
                    "Could not prepare the email: " + safeMessage(e)
            );

            Toast.makeText(
                    this,
                    "Could not attach the backup.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024L * 1024L) {
            return String.format(
                    Locale.US, "%.1f KB", bytes / 1024.0
            );
        }

        return String.format(
                Locale.US, "%.2f MB",
                bytes / (1024.0 * 1024.0)
        );
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.trim().isEmpty()
                ? e.getClass().getSimpleName()
                : message;
    }
}
