package com.aaagrowers.coldroom.stocks;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class GradedStockDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "graded_stock.db";
    private static final int DB_VERSION = 2;

    public GradedStockDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createInventoryTable(db);
        createScansTable(db);
        createFixesTable(db);
    }

    private void createInventoryTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS graded_stock (" +
                "qr TEXT PRIMARY KEY," +
                "serial TEXT," +
                "bucket_name TEXT," +
                "farm_id INTEGER," +
                "variety_id INTEGER," +
                "variety_name TEXT," +
                "length INTEGER," +
                "remaining_bunches INTEGER," +
                "stems_per_bunch INTEGER," +
                "graded_at TEXT" +
                ")");
    }

    private void createScansTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS graded_scans (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "qr TEXT NOT NULL," +
                "farm_id INTEGER NOT NULL," +
                "scanned_at TEXT," +
                "synced INTEGER NOT NULL DEFAULT 0" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_graded_scans_pending " +
                "ON graded_scans(synced, farm_id)");
    }

    private void createFixesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS graded_stock_fixes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "qr TEXT NOT NULL," +
                "farm_id INTEGER NOT NULL," +
                "variety_id INTEGER NOT NULL," +
                "variety_name TEXT NOT NULL," +
                "length INTEGER NOT NULL," +
                "remaining_bunches INTEGER NOT NULL," +
                "stems_per_bunch INTEGER NOT NULL," +
                "queued_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                "synced INTEGER NOT NULL DEFAULT 0," +
                "last_error TEXT," +
                "UNIQUE(qr, farm_id)" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_graded_stock_fixes_pending " +
                "ON graded_stock_fixes(synced, farm_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Preserve existing inventory and pending scans. Only add missing structures.
        if (oldVersion < 2) {
            createInventoryTable(db);
            createScansTable(db);
            createFixesTable(db);
        }
    }
}
