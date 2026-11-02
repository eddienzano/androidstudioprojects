package com.aaagrowers.coldroom;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "coldroom.db";
    private static final int DB_VERSION = 10; // 🔥 increase from 5 → 6

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE qr_inventory (" +
                "qr_code TEXT PRIMARY KEY," +
                "variety_name TEXT NOT NULL," +
                "intake_time TEXT NOT NULL," +
                "farm TEXT NOT NULL," +
                "quantity INTEGER DEFAULT 0)";
        db.execSQL(createTable);


        db.execSQL("CREATE TABLE box_storage_local (" +
                "qr_code TEXT PRIMARY KEY," +
                "box_id INTEGER," +
                "variety_name TEXT," +
                "length INTEGER," +
                "stems INTEGER," +
                "updated_at TEXT," +
                "is_synced INTEGER DEFAULT 1," +
                "last_modified TEXT" +
                ")");

        // ✅ ADD THIS TABLE (THIS IS WHAT YOU MISSED)
        db.execSQL("CREATE TABLE offline_scanout (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "qr_code TEXT UNIQUE," +
                "user_id INTEGER," +
                "synced INTEGER DEFAULT 0," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")");

        // NEW TABLE: offline_dip_scan
        db.execSQL("CREATE TABLE IF NOT EXISTS offline_dip_scan (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "qr_code TEXT UNIQUE," +
                "user_id INTEGER," +
                "synced INTEGER DEFAULT 0" +
                ")");


        db.execSQL("CREATE TABLE IF NOT EXISTS varieties_local (" +
                "varietyid INTEGER PRIMARY KEY," +
                "varietyname TEXT" +
                ")");

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS inventory_fixes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "qr_code TEXT UNIQUE," +
                        "old_variety TEXT," +
                        "new_variety TEXT," +
                        "old_quantity INTEGER," +
                        "new_quantity INTEGER," +
                        "synced INTEGER DEFAULT 0," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ")"
        );
    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Add quantity column if upgrading from older version
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE qr_inventory ADD COLUMN quantity INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE box_storage_local ADD COLUMN is_synced INTEGER DEFAULT 1");
            db.execSQL("ALTER TABLE box_storage_local ADD COLUMN last_modified TEXT");
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS offline_scanout (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "qr_code TEXT," +
                    "user_id INTEGER," +
                    "synced INTEGER DEFAULT 0," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")");
        }
        if (oldVersion < 5) {
            // Ensure new table exists after upgrade
            db.execSQL("CREATE TABLE IF NOT EXISTS offline_dip_scan (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "qr_code TEXT UNIQUE," +
                    "user_id INTEGER," +
                    "synced INTEGER DEFAULT 0" +
                    ")");
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE box_storage_local ADD COLUMN variety_id INTEGER");
        }
        if (oldVersion < 8) {
            db.execSQL("CREATE TABLE IF NOT EXISTS varieties_local (" +
                    "varietyid INTEGER PRIMARY KEY," +
                    "varietyname TEXT" +
                    ")");
        }
        if (oldVersion < 9) {

            db.execSQL("DROP TABLE IF EXISTS varieties_local");

            db.execSQL(
                    "CREATE TABLE varieties_local (" +
                            "varietyid INTEGER PRIMARY KEY," +
                            "varietyname TEXT)"
            );
        }
        if(oldVersion < 10){
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS inventory_fixes (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "qr_code TEXT UNIQUE," +
                            "old_variety TEXT," +
                            "new_variety TEXT," +
                            "old_quantity INTEGER," +
                            "new_quantity INTEGER," +
                            "synced INTEGER DEFAULT 0," +
                            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                            ")"
            );
        }
    }

    // Insert or update QR record
    public void insertQR(String qrCode, String variety, String intakeTime, String farm, int quantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("qr_code", qrCode);
        cv.put("variety_name", variety);
        cv.put("intake_time", intakeTime);
        cv.put("farm", farm);
        cv.put("quantity", quantity);
        db.insertWithOnConflict("qr_inventory", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    // Lookup QR by QR code + farm
    public QRInventory getQR(String qrCode, String farm) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM qr_inventory WHERE qr_code=? AND farm=?",
                new String[]{qrCode, farm});
        if (c.moveToFirst()) {
            QRInventory qr = new QRInventory(
                    c.getString(c.getColumnIndexOrThrow("qr_code")),
                    c.getString(c.getColumnIndexOrThrow("variety_name")),
                    c.getString(c.getColumnIndexOrThrow("intake_time")),
                    c.getString(c.getColumnIndexOrThrow("farm")),
                    c.getInt(c.getColumnIndexOrThrow("quantity"))
            );
            c.close();
            return qr;
        }
        c.close();
        return null;
    }

    public void updateQR(String qrCode,
                         String variety,
                         int quantity){

        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("variety_name", variety);
        cv.put("quantity", quantity);

        db.update(
                "qr_inventory",
                cv,
                "qr_code=?",
                new String[]{qrCode}
        );

        db.close();
    }

    public List<String> getAllVarieties() {

        List<String> list = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT varietyname FROM varieties_local ORDER BY varietyname",
                null
        );

        while (c.moveToNext()) {
            list.add(c.getString(0));
        }

        c.close();

        return list;
    }


    public void saveInventoryFix(
            String qrCode,
            String oldVariety,
            String newVariety,
            int oldQty,
            int newQty
    ) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            ContentValues cv = new ContentValues();
            cv.put("qr_code", qrCode);
            cv.put("old_variety", oldVariety);
            cv.put("new_variety", newVariety);
            cv.put("old_quantity", oldQty);
            cv.put("new_quantity", newQty);
            cv.put("synced", 0);

            db.insertWithOnConflict(
                    "inventory_fixes",
                    null,
                    cv,
                    SQLiteDatabase.CONFLICT_REPLACE
            );
        } finally {
            db.close();
        }
    }

}