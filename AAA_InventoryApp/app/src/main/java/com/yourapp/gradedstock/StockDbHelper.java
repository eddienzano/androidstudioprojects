package com.yourapp.gradedstock;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class StockDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "gradedstock.db";

    /*
     * Version 3
     *
     * Version 2 already exists on the devices.
     * Version 3 adds local bucket release tracking
     * without destroying existing data.
     */
    private static final int DB_VERSION = 3;

    public StockDbHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        /*
         * ==========================================================
         * ACTIVE BUCKETS FROM SERVER
         * ==========================================================
         *
         * This is the local cache of currently active
         * graded stock.
         */
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS graded_active_qrs(" +
                        "graded_stock_id INTEGER PRIMARY KEY," +
                        "qr TEXT," +
                        "serial TEXT," +
                        "bucket_name TEXT," +
                        "farm_id INTEGER," +
                        "length INTEGER," +
                        "variety_id INTEGER," +
                        "bunches INTEGER," +
                        "stems_per_bunch INTEGER)"
        );

        /*
         * ==========================================================
         * GRADED STOCK PENDING SYNC
         * ==========================================================
         *
         * New graded stock waiting to be uploaded.
         */
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS graded_pending(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "qr TEXT," +
                        "graded_stock_id INTEGER," +
                        "serial TEXT," +
                        "bucket_name TEXT," +
                        "farm_id INTEGER," +
                        "length INTEGER," +
                        "variety_id INTEGER," +
                        "bunches INTEGER," +
                        "stems_per_bunch INTEGER," +
                        "synced INTEGER DEFAULT 0)"
        );

        /*
         * ==========================================================
         * VARIETIES
         * ==========================================================
         */
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS varieties(" +
                        "id INTEGER PRIMARY KEY," +
                        "name TEXT," +
                        "farm_id INTEGER)"
        );

        /*
         * ==========================================================
         * OFFLINE SCAN-OUT RECORDS
         * ==========================================================
         */
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS graded_out_local(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "qr TEXT," +
                        "graded_stock_id INTEGER," +
                        "farm_id INTEGER," +
                        "action_type TEXT," +
                        "bunches_removed INTEGER DEFAULT 0," +
                        "scanned_at TEXT," +
                        "device_id TEXT," +
                        "synced INTEGER DEFAULT 0)"
        );

        /*
         * ==========================================================
         * LOCALLY RELEASED BUCKETS
         * ==========================================================
         *
         * This table is important for offline bucket reuse.
         *
         * When a COMPLETE dispatch is saved locally, the bucket
         * is recorded here immediately.
         *
         * The server may still temporarily show the bucket as
         * In-Use until sync_scanouts.php runs, but Android knows
         * that the old stock has already been completed locally.
         */
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS graded_released_qrs(" +
                        "qr TEXT PRIMARY KEY," +
                        "graded_stock_id INTEGER," +
                        "farm_id INTEGER," +
                        "released_at TEXT," +
                        "scanout_local_id INTEGER," +
                        "synced INTEGER DEFAULT 0)"
        );
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ) {

        /*
         * ==========================================================
         * VERSION 2
         * ==========================================================
         *
         * The original application upgraded from version 1
         * by rebuilding the database.
         *
         * Keep this behavior for devices that are still on v1.
         */
        if (oldVersion < 2) {

            db.execSQL(
                    "DROP TABLE IF EXISTS graded_active_qrs"
            );

            db.execSQL(
                    "DROP TABLE IF EXISTS graded_pending"
            );

            db.execSQL(
                    "DROP TABLE IF EXISTS varieties"
            );

            db.execSQL(
                    "DROP TABLE IF EXISTS graded_out_local"
            );

            db.execSQL(
                    "DROP TABLE IF EXISTS graded_released_qrs"
            );

            onCreate(db);

            return;
        }

        /*
         * ==========================================================
         * VERSION 3
         * ==========================================================
         *
         * Do NOT drop the existing tables.
         *
         * Existing scan-outs and pending graded stock must survive
         * the application upgrade.
         */
        if (oldVersion < 3) {

            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS graded_released_qrs(" +
                            "qr TEXT PRIMARY KEY," +
                            "graded_stock_id INTEGER," +
                            "farm_id INTEGER," +
                            "released_at TEXT," +
                            "scanout_local_id INTEGER," +
                            "synced INTEGER DEFAULT 0)"
            );
        }
    }
}