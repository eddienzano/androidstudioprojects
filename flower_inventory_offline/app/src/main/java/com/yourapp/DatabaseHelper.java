package com.yourapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "flower_inventory_offline.db";
    private static final int DATABASE_VERSION = 2; // bump version because we add a new table

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create farm table
        db.execSQL("CREATE TABLE IF NOT EXISTS farm (" +
                "farmid INTEGER PRIMARY KEY, " +
                "farmname TEXT)");

        // Create greenhouses table
        db.execSQL("CREATE TABLE IF NOT EXISTS greenhouses (" +
                "id INTEGER PRIMARY KEY, " +
                "GreenhouseId INTEGER, " +
                "GreenhouseName TEXT, " +
                "Display_Name TEXT, " +
                "FarmId INTEGER, " +
                "VarietyId INTEGER)");

        // Create variety table
        db.execSQL("CREATE TABLE IF NOT EXISTS variety (" +
                "VarietyId INTEGER PRIMARY KEY, " +
                "VarietyName TEXT, " +
                "FarmId INTEGER)");

        // Create rejection_reason table
        db.execSQL("CREATE TABLE IF NOT EXISTS rejection_reason (" +
                "rejection_id INTEGER PRIMARY KEY, " +
                "category TEXT, " +
                "abbreviation TEXT, " +
                "priority INTEGER)");

        // Create rejections table
        db.execSQL("CREATE TABLE IF NOT EXISTS rejections (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "farm INTEGER, " +
                "variety_id INTEGER, " +
                "variety_name TEXT, " +
                "stems INTEGER, " +
                "rejection_reason INTEGER, " +
                "submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop tables if upgrading
        db.execSQL("DROP TABLE IF EXISTS rejections");
        db.execSQL("DROP TABLE IF EXISTS rejection_reason");
        db.execSQL("DROP TABLE IF EXISTS greenhouses");
        db.execSQL("DROP TABLE IF EXISTS variety"); // drop new table too
        db.execSQL("DROP TABLE IF EXISTS farm");
        onCreate(db);
    }
}
