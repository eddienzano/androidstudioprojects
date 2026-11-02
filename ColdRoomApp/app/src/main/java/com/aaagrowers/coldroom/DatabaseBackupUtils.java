
package com.aaagrowers.coldroom;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class DatabaseBackupUtils {

    private static final String BACKUP_PREFIX = "coldroom_backup_";

    /*
     * All database files identified in the supplied helpers.
     * A database is included only if its file already exists.
     */
    private static final String[] DATABASE_NAMES = {
            "coldroom.db",
            "flower_inventory.db",
            "stock_inventory.db",
            "stockscan.db",
            "variety.db",
            "summers.db"
    };

    private DatabaseBackupUtils() {
    }

    public static File getBackupDirectory(Context context) {
        File directory = new File(
                context.getFilesDir(),
                "database_backups"
        );

        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create the backup directory."
            );
        }

        return directory;
    }

    /**
     * Creates one ZIP archive containing all existing databases.
     *
     * Call while database writes are stopped. This method cannot
     * close connections owned by other activities or helpers.
     */
    public static synchronized File createBackup(
            Context context,
            String prefix
    ) throws Exception {

        File databaseDirectory = context.getDatabasePath(
                DATABASE_NAMES[0]
        ).getParentFile();

        if (databaseDirectory == null ||
                !databaseDirectory.exists()) {
            throw new IOException(
                    "The application database directory is unavailable."
            );
        }

        List<File> existingDatabases = new ArrayList<>();

        for (String name : DATABASE_NAMES) {
            File databaseFile = new File(databaseDirectory, name);

            if (databaseFile.exists() && databaseFile.length() > 0) {
                checkpointDatabase(databaseFile);
                validateSQLiteFile(databaseFile);
                existingDatabases.add(databaseFile);
            }
        }

        if (existingDatabases.isEmpty()) {
            throw new IOException(
                    "No existing database files were found. " +
                            "Open the relevant app features first so their " +
                            "databases can be created."
            );
        }

        File backupDirectory = getBackupDirectory(context);

        String timestamp = new java.text.SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                java.util.Locale.US
        ).format(new java.util.Date());

        String safePrefix = prefix == null || prefix.trim().isEmpty()
                ? BACKUP_PREFIX
                : prefix;

        File archive = new File(
                backupDirectory,
                safePrefix + timestamp + ".zip"
        );

        int suffix = 1;

        while (archive.exists()) {
            archive = new File(
                    backupDirectory,
                    safePrefix + timestamp + "_" + suffix + ".zip"
            );
            suffix++;
        }

        try (
                FileOutputStream fileOutput =
                        new FileOutputStream(archive);
                BufferedOutputStream bufferedOutput =
                        new BufferedOutputStream(fileOutput);
                ZipOutputStream zipOutput =
                        new ZipOutputStream(bufferedOutput)
        ) {
            for (File databaseFile : existingDatabases) {
                ZipEntry entry = new ZipEntry(databaseFile.getName());
                zipOutput.putNextEntry(entry);

                try (FileInputStream input =
                             new FileInputStream(databaseFile)) {

                    byte[] buffer = new byte[8192];
                    int count;

                    while ((count = input.read(buffer)) != -1) {
                        zipOutput.write(buffer, 0, count);
                    }
                }

                zipOutput.closeEntry();
            }

            zipOutput.finish();
            zipOutput.flush();
            fileOutput.getFD().sync();

        } catch (Exception e) {
            //noinspection ResultOfMethodCallIgnored
            archive.delete();
            throw e;
        }

        try {
            validateDatabase(archive);
        } catch (Exception e) {
            //noinspection ResultOfMethodCallIgnored
            archive.delete();
            throw e;
        }

        return archive;
    }

    private static void checkpointDatabase(File file) throws Exception {
        SQLiteDatabase database = null;

        try {
            database = SQLiteDatabase.openDatabase(
                    file.getAbsolutePath(),
                    null,
                    SQLiteDatabase.OPEN_READWRITE
            );

            try (Cursor cursor = database.rawQuery(
                    "PRAGMA wal_checkpoint(TRUNCATE)",
                    null
            )) {
                if (cursor.moveToFirst()) {
                    int busy = cursor.getInt(0);

                    if (busy != 0) {
                        throw new IOException(
                                "Database is busy and could not be " +
                                        "checkpointed: " + file.getName() +
                                        ". Stop database operations and retry."
                        );
                    }
                }
            }

        } finally {
            if (database != null) {
                database.close();
            }
        }
    }

    public static synchronized List<File> getBackups(Context context) {
        File directory = getBackupDirectory(context);

        File[] files = directory.listFiles((dir, name) ->
                name.startsWith(BACKUP_PREFIX) &&
                        name.endsWith(".zip")
        );

        List<File> backups = new ArrayList<>();

        if (files != null) {
            backups.addAll(Arrays.asList(files));
        }

        Collections.sort(backups, (a, b) ->
                Long.compare(b.lastModified(), a.lastModified())
        );

        return backups;
    }

    /**
     * Validates a ZIP backup archive and every SQLite database inside.
     * Also accepts an individual SQLite database file for compatibility.
     */
    public static void validateDatabase(File file) throws Exception {
        if (file == null || !file.exists() || file.length() == 0) {
            throw new IOException("The backup is empty or missing.");
        }

        if (!file.getName().toLowerCase(
                java.util.Locale.US
        ).endsWith(".zip")) {
            validateSQLiteFile(file);
            return;
        }

        File temporaryDirectory = new File(
                file.getParentFile(),
                "validate_" + System.nanoTime()
        );

        if (!temporaryDirectory.mkdirs()) {
            throw new IOException(
                    "Could not create a temporary validation directory."
            );
        }

        int databaseCount = 0;

        try (ZipInputStream zipInput = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {

            ZipEntry entry;

            while ((entry = zipInput.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zipInput.closeEntry();
                    continue;
                }

                String name = new File(entry.getName()).getName();

                if (!isSupportedDatabase(name)) {
                    throw new IOException(
                            "Unsupported file in backup: " + name
                    );
                }

                File extracted = new File(temporaryDirectory, name);

                try (FileOutputStream output =
                             new FileOutputStream(extracted)) {
                    byte[] buffer = new byte[8192];
                    int count;

                    while ((count = zipInput.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }

                    output.flush();
                    output.getFD().sync();
                }

                validateSQLiteFile(extracted);
                databaseCount++;
                zipInput.closeEntry();
            }
        } finally {
            deleteRecursively(temporaryDirectory);
        }

        if (databaseCount == 0) {
            throw new IOException(
                    "The archive contains no supported database files."
            );
        }
    }

    private static boolean isSupportedDatabase(String name) {
        for (String databaseName : DATABASE_NAMES) {
            if (databaseName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static void validateSQLiteFile(File file) throws Exception {
        if (!file.exists() || file.length() == 0) {
            throw new IOException(
                    "Database file is empty or missing: " + file.getName()
            );
        }

        SQLiteDatabase database = null;

        try {
            database = SQLiteDatabase.openDatabase(
                    file.getAbsolutePath(),
                    null,
                    SQLiteDatabase.OPEN_READONLY
            );

            try (Cursor cursor = database.rawQuery(
                    "PRAGMA integrity_check",
                    null
            )) {
                if (!cursor.moveToFirst() ||
                        !"ok".equalsIgnoreCase(cursor.getString(0))) {
                    throw new IOException(
                            "SQLite integrity check failed for " +
                                    file.getName()
                    );
                }
            }

        } finally {
            if (database != null) {
                database.close();
            }
        }
    }

    /**
     * Restores every database included in the selected ZIP.
     *
     * All database connections must be closed before calling this.
     * A safety archive is created first. Each existing target is
     * preserved as a temporary file until replacement succeeds.
     */
    public static synchronized File restoreBackup(
            Context context,
            File selectedBackup
    ) throws Exception {

        validateDatabase(selectedBackup);

        File safetyBackup = createBackup(
                context,
                "coldroom_safety_"
        );

        File databaseDirectory = context.getDatabasePath(
                DATABASE_NAMES[0]
        ).getParentFile();

        File restoreDirectory = new File(
                context.getCacheDir(),
                "database_restore_" + System.nanoTime()
        );

        if (!restoreDirectory.mkdirs()) {
            throw new IOException(
                    "Could not create the restore staging directory."
            );
        }

        List<File> stagedDatabases = new ArrayList<>();
        List<File> replacedTargets = new ArrayList<>();

        try {
            /*
             * Extract only known database names.
             */
            try (ZipInputStream zipInput = new ZipInputStream(
                    new BufferedInputStream(
                            new FileInputStream(selectedBackup)))) {

                ZipEntry entry;

                while ((entry = zipInput.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        zipInput.closeEntry();
                        continue;
                    }

                    String name = new File(entry.getName()).getName();

                    if (!isSupportedDatabase(name)) {
                        throw new IOException(
                                "Unsupported database file: " + name
                        );
                    }

                    File staged = new File(restoreDirectory, name);

                    try (FileOutputStream output =
                                 new FileOutputStream(staged)) {

                        byte[] buffer = new byte[8192];
                        int count;

                        while ((count = zipInput.read(buffer)) != -1) {
                            output.write(buffer, 0, count);
                        }

                        output.flush();
                        output.getFD().sync();
                    }

                    validateSQLiteFile(staged);
                    stagedDatabases.add(staged);
                    zipInput.closeEntry();
                }
            }

            if (stagedDatabases.isEmpty()) {
                throw new IOException(
                        "No databases were found in the selected backup."
                );
            }

            /*
             * Move existing target databases aside, then install
             * the validated replacement files.
             */
            for (File staged : stagedDatabases) {
                File target = new File(
                        databaseDirectory,
                        staged.getName()
                );

                File previous = new File(
                        databaseDirectory,
                        staged.getName() + ".restore_previous"
                );

                deleteIfExists(previous);

                if (target.exists()) {
                    if (!target.renameTo(previous)) {
                        throw new IOException(
                                "Could not preserve current database: " +
                                        target.getName()
                        );
                    }
                    replacedTargets.add(previous);
                } else {
                    replacedTargets.add(previous);
                }

                File replacement = new File(
                        databaseDirectory,
                        staged.getName() + ".restore_new"
                );

                deleteIfExists(replacement);
                copyFile(staged, replacement);

                if (!replacement.renameTo(target)) {
                    deleteIfExists(replacement);
                    throw new IOException(
                            "Could not install database: " +
                                    target.getName()
                    );
                }

                validateSQLiteFile(target);
            }

            /*
             * Remove sidecar files only after all connections have
             * been closed by the calling application.
             */
            for (File staged : stagedDatabases) {
                String path = new File(
                        databaseDirectory,
                        staged.getName()
                ).getAbsolutePath();

                deleteIfExists(new File(path + "-wal"));
                deleteIfExists(new File(path + "-shm"));
                deleteIfExists(new File(path + "-journal"));
            }

            for (File previous : replacedTargets) {
                deleteIfExists(previous);
            }

            return safetyBackup;

        } catch (Exception e) {
            /*
             * Roll back targets for which a previous file exists.
             */
            for (File previous : replacedTargets) {
                if (previous.exists()) {
                    String originalName = previous.getName().replace(
                            ".restore_previous",
                            ""
                    );

                    File target = new File(
                            databaseDirectory,
                            originalName
                    );

                    //noinspection ResultOfMethodCallIgnored
                    target.delete();

                    if (!previous.renameTo(target)) {
                        e.addSuppressed(new IOException(
                                "Could not roll back " + originalName
                        ));
                    }
                }
            }

            throw e;

        } finally {
            deleteRecursively(restoreDirectory);
        }
    }

    public static void copyFile(
            File source,
            File destination
    ) throws IOException {

        File parent = destination.getParentFile();

        if (parent != null &&
                !parent.exists() &&
                !parent.mkdirs()) {
            throw new IOException(
                    "Could not create destination directory."
            );
        }

        try (
                FileInputStream input = new FileInputStream(source);
                FileOutputStream output = new FileOutputStream(destination)
        ) {
            byte[] buffer = new byte[8192];
            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }

            output.flush();
            output.getFD().sync();
        }
    }

    private static void deleteIfExists(File file) throws IOException {
        if (file.exists() && !file.delete()) {
            throw new IOException(
                    "Could not delete file: " + file.getAbsolutePath()
            );
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();

            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }

        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
