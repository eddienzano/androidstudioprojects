
package com.aaagrowers.coldroom;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseRestoreActivity extends AppCompatActivity {

    private LinearLayout root;
    private LinearLayout backupList;
    private TextView statusText;

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    this::importExternalBackup
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildInterface();
        refreshBackups();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (backupList != null) {
            refreshBackups();
        }
    }

    private void buildInterface() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(246, 248, 251));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(28));

        scrollView.addView(root);

        TextView heading = new TextView(this);
        heading.setText("Database Restore");
        heading.setTextSize(27);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(Color.rgb(28, 40, 58));
        root.addView(heading);

        TextView subtitle = new TextView(this);
        subtitle.setText(
                "Manage your saved database backups and restore a previous copy."
        );
        subtitle.setTextSize(15);
        subtitle.setTextColor(Color.rgb(100, 112, 130));
        subtitle.setPadding(0, dp(7), 0, dp(20));
        root.addView(subtitle);

        LinearLayout warningCard = card();
        warningCard.setBackground(background(
                Color.rgb(255, 248, 230),
                Color.rgb(244, 219, 164),
                14
        ));

        TextView warningTitle = new TextView(this);
        warningTitle.setText("Important");
        warningTitle.setTextSize(16);
        warningTitle.setTypeface(null, Typeface.BOLD);
        warningTitle.setTextColor(Color.rgb(130, 84, 15));
        warningCard.addView(warningTitle);

        TextView warningBody = new TextView(this);
        warningBody.setText(
                "Restoring replaces the databases contained in the selected " +
                        "backup. A safety backup is created first. Stop database " +
                        "operations and close and reopen the app after restoring."
        );
        warningBody.setTextSize(14);
        warningBody.setTextColor(Color.rgb(105, 79, 39));
        warningBody.setPadding(0, dp(7), 0, 0);
        warningCard.addView(warningBody);

        root.addView(warningCard);

        LinearLayout actionCard = card();

        TextView actionTitle = new TextView(this);
        actionTitle.setText("Import a Backup");
        actionTitle.setTextSize(18);
        actionTitle.setTypeface(null, Typeface.BOLD);
        actionTitle.setTextColor(Color.rgb(28, 40, 58));
        actionCard.addView(actionTitle);

        TextView actionDescription = new TextView(this);
        actionDescription.setText(
                "Select a backup ZIP received by email or saved in Downloads."
        );
        actionDescription.setTextSize(14);
        actionDescription.setTextColor(Color.rgb(100, 112, 130));
        actionDescription.setPadding(0, dp(6), 0, dp(14));
        actionCard.addView(actionDescription);

        Button importButton = new Button(this);
        importButton.setText("Choose Backup File");
        importButton.setAllCaps(false);
        importButton.setTextSize(15);
        importButton.setOnClickListener(v ->
                importLauncher.launch(new String[]{"*/*"})
        );
        actionCard.addView(importButton);

        root.addView(actionCard);

        LinearLayout listCard = card();

        TextView listTitle = new TextView(this);
        listTitle.setText("Available Backups");
        listTitle.setTextSize(20);
        listTitle.setTypeface(null, Typeface.BOLD);
        listTitle.setTextColor(Color.rgb(28, 40, 58));
        listCard.addView(listTitle);

        TextView listDescription = new TextView(this);
        listDescription.setText(
                "Select a saved backup to review it and restore it."
        );
        listDescription.setTextSize(14);
        listDescription.setTextColor(Color.rgb(100, 112, 130));
        listDescription.setPadding(0, dp(5), 0, dp(15));
        listCard.addView(listDescription);

        backupList = new LinearLayout(this);
        backupList.setOrientation(LinearLayout.VERTICAL);
        listCard.addView(backupList);

        root.addView(listCard);

        statusText = new TextView(this);
        statusText.setTextSize(13);
        statusText.setTextColor(Color.rgb(75, 91, 110));
        statusText.setPadding(dp(4), dp(10), dp(4), dp(8));
        root.addView(statusText);

        setContentView(scrollView);
    }

    private void refreshBackups() {
        if (backupList == null) {
            return;
        }

        backupList.removeAllViews();

        List<File> backups = DatabaseBackupUtils.getBackups(this);

        if (backups == null || backups.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(
                    "No backups found.\n\n" +
                            "Import a backup file above or create a backup " +
                            "from the Database Backup screen."
            );
            empty.setTextSize(14);
            empty.setTextColor(Color.rgb(100, 112, 130));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(16), dp(25), dp(16), dp(25));

            backupList.addView(empty);

            statusText.setText("No saved backups available.");
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "dd MMM yyyy, HH:mm",
                Locale.getDefault()
        );

        for (File backup : backups) {
            if (backup == null || !backup.isFile()) {
                continue;
            }

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(14), dp(14), dp(14), dp(14));
            item.setBackground(background(
                    Color.WHITE,
                    Color.rgb(225, 231, 239),
                    12
            ));

            LinearLayout.LayoutParams itemParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
            itemParams.bottomMargin = dp(12);

            TextView fileName = new TextView(this);
            fileName.setText(backup.getName());
            fileName.setTextSize(15);
            fileName.setTypeface(null, Typeface.BOLD);
            fileName.setTextColor(Color.rgb(28, 40, 58));
            item.addView(fileName);

            TextView details = new TextView(this);
            details.setText(
                    dateFormat.format(new Date(backup.lastModified())) +
                            "\n" + formatBytes(backup.length())
            );
            details.setTextSize(13);
            details.setTextColor(Color.rgb(100, 112, 130));
            details.setPadding(0, dp(6), 0, dp(12));
            item.addView(details);

            Button restoreButton = new Button(this);
            restoreButton.setText("Restore This Backup");
            restoreButton.setAllCaps(false);
            restoreButton.setOnClickListener(v ->
                    confirmRestore(backup)
            );
            item.addView(restoreButton);

            backupList.addView(item, itemParams);
        }

        statusText.setText(
                backupList.getChildCount() + " saved backup(s) available."
        );
    }

    private void importExternalBackup(Uri uri) {
        if (uri == null) {
            return;
        }

        File importedFile = null;

        try {
            // Permanent app storage, shared with the backup listing.
            File backupDirectory = new File(
                    getFilesDir(),
                    "database_backups"
            );

            if (!backupDirectory.exists() && !backupDirectory.mkdirs()) {
                throw new java.io.IOException(
                        "Could not create the permanent backup directory."
                );
            }

            importedFile = new File(
                    backupDirectory,
                    "coldroom_backup_imported_" +
                            System.currentTimeMillis() + ".zip"
            );

            try (
                    InputStream input =
                            getContentResolver().openInputStream(uri);
                    FileOutputStream output =
                            new FileOutputStream(importedFile)
            ) {
                if (input == null) {
                    throw new java.io.IOException(
                            "Could not read the selected file."
                    );
                }

                byte[] buffer = new byte[8192];
                int count;

                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }

                output.flush();
                output.getFD().sync();
            }

            if (importedFile.length() < 4) {
                throw new java.io.IOException(
                        "The selected file is empty or invalid."
                );
            }

            // This screen imports full app backup ZIP files.
            byte[] header = new byte[4];

            try (FileInputStream input =
                         new FileInputStream(importedFile)) {
                if (input.read(header) != 4 ||
                        header[0] != 'P' ||
                        header[1] != 'K' ||
                        !(
                                (header[2] == 3 && header[3] == 4) ||
                                        (header[2] == 5 && header[3] == 6) ||
                                        (header[2] == 7 && header[3] == 8)
                        )) {
                    throw new java.io.IOException(
                            "This is not a valid ZIP backup. " +
                                    "Choose the ZIP backup created by the app."
                    );
                }
            }

            // Validate archive contents before offering restoration.
            DatabaseBackupUtils.validateDatabase(importedFile);

            refreshBackups();

            statusText.setText(
                    "Backup imported and saved permanently:\n" +
                            importedFile.getName()
            );

            Toast.makeText(
                    this,
                    "Backup imported successfully.",
                    Toast.LENGTH_LONG
            ).show();

            confirmRestore(importedFile);

        } catch (Exception e) {
            if (importedFile != null && importedFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                importedFile.delete();
            }

            refreshBackups();

            new AlertDialog.Builder(this)
                    .setTitle("Import Failed")
                    .setMessage(
                            "The backup could not be imported or validated.\n\n" +
                                    safeMessage(e)
                    )
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void confirmRestore(File selectedBackup) {
        if (selectedBackup == null || !selectedBackup.isFile()) {
            showError("The selected backup file could not be found.");
            refreshBackups();
            return;
        }

        try {
            DatabaseBackupUtils.validateDatabase(selectedBackup);
        } catch (Exception e) {
            showError("Invalid backup:\n\n" + safeMessage(e));
            refreshBackups();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Database Restore")
                .setMessage(
                        "Backup:\n" + selectedBackup.getName() +
                                "\n\nSize: " + formatBytes(selectedBackup.length()) +
                                "\n\nThe current databases will be replaced by " +
                                "the databases in this backup. A safety backup " +
                                "will be created first.\n\nContinue?"
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Restore", (dialog, which) ->
                        performRestore(selectedBackup)
                )
                .show();
    }

    private void performRestore(File selectedBackup) {
        try {
            File safetyBackup = DatabaseBackupUtils.restoreBackup(
                    this,
                    selectedBackup
            );

            statusText.setText(
                    "Restore completed.\n\nSafety backup: " +
                            safetyBackup.getName() +
                            "\n\nClose and reopen the application before continuing."
            );

            refreshBackups();

            new AlertDialog.Builder(this)
                    .setTitle("Restore Completed")
                    .setMessage(
                            "The restore operation reported success.\n\n" +
                                    "Safety backup:\n" + safetyBackup.getName() +
                                    "\n\nClose and reopen the app before continuing."
                    )
                    .setPositiveButton("OK", null)
                    .show();

        } catch (Exception e) {
            statusText.setText(
                    "Restore failed: " + safeMessage(e)
            );

            showError(
                    "The database could not be restored.\n\n" +
                            safeMessage(e)
            );
        }
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(18), dp(16), dp(18));
        card.setBackground(background(
                Color.WHITE,
                Color.rgb(231, 235, 241),
                16
        ));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        params.bottomMargin = dp(16);

        card.setLayoutParams(params);
        return card;
    }

    private GradientDrawable background(
            int fillColor,
            int strokeColor,
            int radiusDp
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024L * 1024L) {
            return String.format(
                    Locale.getDefault(),
                    "%.1f KB",
                    bytes / 1024.0
            );
        }

        return String.format(
                Locale.getDefault(),
                "%.2f MB",
                bytes / (1024.0 * 1024.0)
        );
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.trim().isEmpty()
                ? "An unexpected error occurred."
                : message;
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Database Restore")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
