package com.aaagrowers.coldroom.storage;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FlowerDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "flower_inventory.db";

    // 🔥 bump version when schema changes
    private static final int DB_VERSION = 6;

    public FlowerDbHelper(Context c) {
        super(c, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // =============================
        // FARMS
        // =============================
        db.execSQL("CREATE TABLE IF NOT EXISTS farms_local (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT)");

        // =============================
        // VARIETIES
        // =============================
        db.execSQL("CREATE TABLE IF NOT EXISTS varieties_local (" +
                "id INTEGER PRIMARY KEY," +
                "farm_id INTEGER," +
                "name TEXT)");

        // =============================
        // BOXES (USED BY BOX FILLING ONLY)
        // =============================
        db.execSQL("CREATE TABLE IF NOT EXISTS boxes_local (" +
                "id INTEGER PRIMARY KEY," +
                "qr_code TEXT UNIQUE," +
                "farm_id INTEGER," +
                "status TEXT," +
                "synced INTEGER DEFAULT 0)");

        // =============================
        // BOX CONTENTS
        // =============================
        db.execSQL("CREATE TABLE IF NOT EXISTS box_contents_local (" +
                "box_id INTEGER PRIMARY KEY," +
                "variety_id INTEGER," +
                "length INTEGER," +
                "number_of_stems INTEGER," +
                "updated_at TEXT," +
                "synced INTEGER DEFAULT 0)");

        // =============================
        // 🔥 FILLED BOXES (READ-ONLY FOR STOCK SCAN)
        // =============================
        db.execSQL("CREATE TABLE IF NOT EXISTS filled_boxes_local (" +
                "id INTEGER PRIMARY KEY," +
                "qr_code TEXT UNIQUE," +
                "farm_id INTEGER)");

        db.execSQL("CREATE TABLE warehouse_scan_sessions_local (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "farm_id INTEGER," +
                "scan_date TEXT," +
                "started_at TEXT," +
                "ended_at TEXT," +
                "status TEXT," +
                "server_session_id INTEGER)");

        // =============================
        // 🔥 WAREHOUSE STOCK SCANS
        // =============================
        db.execSQL("CREATE TABLE IF NOT EXISTS warehouse_scans_local (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "farm_id INTEGER," +
                "box_id INTEGER," +
                "qr_code TEXT," +
                "scanned_at TEXT," +
                "synced INTEGER DEFAULT 0," +
                "UNIQUE(box_id))");

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_scans_box ON warehouse_scans_local(box_id)");
    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {

        // =========================================
        // 🔥 VERSION 6 UPGRADE (STOCK SCAN STRUCTURE)
        // =========================================
        if (oldV < 6) {

            // Create safely (no DROP unless necessary)
            db.execSQL("CREATE TABLE IF NOT EXISTS filled_boxes_local (" +
                    "id INTEGER PRIMARY KEY," +
                    "qr_code TEXT UNIQUE," +
                    "farm_id INTEGER)");

            db.execSQL("CREATE TABLE IF NOT EXISTS warehouse_scans_local (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "farm_id INTEGER," +
                    "box_id INTEGER," +
                    "qr_code TEXT," +
                    "scanned_at TEXT," +
                    "synced INTEGER DEFAULT 0," +
                    "UNIQUE(box_id))");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_scans_box ON warehouse_scans_local(box_id)");
        }

        // =========================================
        // 🔁 VERSION 5 (BOX CONTENT FIX)
        // =========================================
        if (oldV < 5) {

            db.execSQL("DROP TABLE IF EXISTS box_contents_local");

            db.execSQL("CREATE TABLE box_contents_local (" +
                    "box_id INTEGER PRIMARY KEY," +
                    "variety_id INTEGER," +
                    "length INTEGER," +
                    "number_of_stems INTEGER," +
                    "updated_at TEXT," +
                    "synced INTEGER DEFAULT 0)");
        }
    }

    // =============================
    // SEED DATA
    // =============================
    public void seedIfEmpty(SQLiteDatabase db) {

        Cursor c = db.rawQuery("SELECT COUNT(*) FROM farms_local", null);
        c.moveToFirst();

        if (c.getInt(0) == 0) {
            db.execSQL("INSERT INTO farms_local VALUES (1,'Simba')");
            db.execSQL("INSERT INTO farms_local VALUES (2,'Chui')");
        }

        c.close();
    }
}