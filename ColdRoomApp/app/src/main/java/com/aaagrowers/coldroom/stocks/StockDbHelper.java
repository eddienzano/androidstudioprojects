package com.aaagrowers.coldroom.stocks;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class StockDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "stockscan.db";
    private static final int DB_VERSION = 4; // 🔥 bumped

    public StockDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // ✅ FIXED: removed UNIQUE + added is_duplicate
        db.execSQL(
                "CREATE TABLE pending_scans (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "qr_code TEXT," +
                        "is_duplicate INTEGER DEFAULT 0," +
                        "synced INTEGER DEFAULT 0," +
                        "scan_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ")"
        );

        db.execSQL(
                "CREATE TABLE stock_take (" +
                        "qr_code TEXT PRIMARY KEY," +
                        "scan_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ")"
        );

        db.execSQL(
                "CREATE TABLE fixed_scans (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "qr_code TEXT," +
                        "variety_id INTEGER," +
                        "variety_name TEXT," +
                        "quantity INTEGER," +
                        "farm TEXT," +
                        "synced INTEGER DEFAULT 0," +
                        "scan_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ")"
        );

        db.execSQL(
                "CREATE TABLE graded_inventory (" +
                        "qr TEXT PRIMARY KEY," +
                        "serial TEXT," +
                        "bucket_name TEXT," +
                        "farm TEXT," +
                        "variety_id INTEGER," +
                        "variety_name TEXT," +
                        "length INTEGER," +
                        "bunches INTEGER," +
                        "stems_per_bunch INTEGER," +
                        "graded_at TEXT" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if(oldVersion < 2){
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS stock_take (" +
                            "qr_code TEXT PRIMARY KEY," +
                            "scan_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                            ")"
            );
        }

        if(oldVersion < 3){
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS fixed_scans (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "qr_code TEXT," +
                            "variety_id INTEGER," +
                            "variety_name TEXT," +
                            "quantity INTEGER," +
                            "farm TEXT," +
                            "synced INTEGER DEFAULT 0," +
                            "scan_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                            ")"
            );
        }

        if (oldVersion < 4) {

            // ⚠️ IMPORTANT: recreate pending_scans properly
            db.execSQL("DROP TABLE IF EXISTS pending_scans");

            db.execSQL(
                    "CREATE TABLE pending_scans (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "qr_code TEXT," +
                            "is_duplicate INTEGER DEFAULT 0," +
                            "synced INTEGER DEFAULT 0," +
                            "scan_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                            ")"
            );

            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS graded_inventory (" +
                            "qr TEXT PRIMARY KEY," +
                            "serial TEXT," +
                            "bucket_name TEXT," +
                            "farm TEXT," +
                            "variety_id INTEGER," +
                            "variety_name TEXT," +
                            "length INTEGER," +
                            "bunches INTEGER," +
                            "stems_per_bunch INTEGER," +
                            "graded_at TEXT" +
                            ")"
            );
        }
    }
}