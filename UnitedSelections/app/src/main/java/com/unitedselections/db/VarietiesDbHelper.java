package com.unitedselections.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class VarietiesDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "varieties.db";

    // IMPORTANT:
    // Increase version so Android recreates database
    private static final int DB_VERSION = 2;

    public VarietiesDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS varieties_cache (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "pedigree_item TEXT," +
                        "description TEXT," +
                        "category TEXT" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS varieties_cache");

        onCreate(db);
    }
}