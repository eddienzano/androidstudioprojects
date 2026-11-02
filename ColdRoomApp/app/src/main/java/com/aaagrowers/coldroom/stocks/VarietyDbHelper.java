package com.aaagrowers.coldroom.stocks;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class VarietyDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "variety.db";
    private static final int DB_VERSION = 1;

    public VarietyDbHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE variety (" +
                        "variety_id INTEGER PRIMARY KEY," +
                        "variety_name TEXT NOT NULL" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}
}