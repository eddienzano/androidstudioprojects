package com.aaagrowers.coldroom.stocks;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class StockInventoryDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "stock_inventory.db";
    private static final int DB_VERSION = 1;

    public StockInventoryDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE active_inventory (" +
                        "qr_code TEXT PRIMARY KEY," +
                        "variety_name TEXT NOT NULL," +
                        "quantity INTEGER DEFAULT 0," +
                        "farm TEXT NOT NULL," +
                        "intake_time TEXT NOT NULL" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}