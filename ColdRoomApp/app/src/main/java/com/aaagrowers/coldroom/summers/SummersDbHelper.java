package com.aaagrowers.coldroom.summers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class SummersDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "summers.db";
    private static final int DB_VERSION = 9;

    public SummersDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // =====================================================
        // INVENTORY CACHE (SERVER DATA ONLY)
        // =====================================================
        db.execSQL(
                "CREATE TABLE inventory (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "qr_code TEXT NOT NULL," +
                        "variety_name TEXT," +
                        "quantity INTEGER," +
                        "intake_time TEXT," +
                        "farm TEXT NOT NULL," +
                        "UNIQUE(qr_code,variety_name,quantity,intake_time,farm)" +
                        ")"
        );

        // =====================================================
        // STOCKTAKE SCANS (ONLY QR CODES)
        // =====================================================
        db.execSQL(
                "CREATE TABLE scans (\n" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                        "    qr_code TEXT NOT NULL,\n" +
                        "    farm TEXT NOT NULL,\n" +
                        "    is_duplicate INTEGER DEFAULT 0,\n" +
                        "    scanned_at DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                        "    synced INTEGER DEFAULT 0\n" +
                        ");"
        );

        //qr master
        db.execSQL(
                "CREATE TABLE qr_master (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "serial TEXT," +
                        "qr_code TEXT," +   // 🔥 from buckets_summers
                        "bucket_name TEXT," +
                        "farm TEXT," +
                        "length INTEGER," +
                        "variety_name TEXT," +
                        "quantity INTEGER," +
                        "batch_id TEXT," +
                        "source_table TEXT," +
                        "last_sync DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ")"
        );

        db.execSQL(
                "CREATE TABLE verify_scans (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "serial TEXT UNIQUE," +
                        "verified_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "synced INTEGER DEFAULT 0" +
                        ")"
        );

        db.execSQL(
                "CREATE TABLE summers_varieties (" +
                        "variety_id INTEGER PRIMARY KEY," +
                        "variety_name TEXT," +
                        "code TEXT," +
                        "type TEXT," +
                        "colour TEXT," +
                        "flower_type_id TEXT" +
                        ")"
        );

        db.execSQL(
                "CREATE TABLE qr_fix_header (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "qr_code TEXT UNIQUE," +
                        "serial TEXT," +
                        "farm TEXT," +
                        "length INTEGER," +
                        "bucket_name TEXT," +
                        "synced INTEGER DEFAULT 0," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ")"
        );

        db.execSQL(
                "CREATE TABLE qr_fix_items (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "qr_code TEXT," +
                        "variety_id INTEGER," +
                        "variety_name TEXT," +
                        "quantity INTEGER," +
                        "synced INTEGER DEFAULT 0" +
                        ")"
        );

        db.execSQL(
                "CREATE TABLE inventory_fixes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "batch_id TEXT," +
                        "old_variety TEXT," +
                        "old_qty INTEGER," +
                        "new_variety TEXT," +
                        "new_qty INTEGER," +
                        "synced INTEGER DEFAULT 0" +
                        ")"
        );


    }

    public void clearVarieties() {

        getWritableDatabase().execSQL(
                "DELETE FROM summers_varieties"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS scans");
        db.execSQL("DROP TABLE IF EXISTS inventory");
        onCreate(db);
    }

    // =====================================================
    // INVENTORY SYNC (FAST CACHE ONLY)
    // =====================================================

    public void clearInventory(String farm) {
        getWritableDatabase().delete("inventory", "farm=?", new String[]{farm});
    }

    public void insertInventory(String qr, String variety, int qty, String time, String farm) {
        ContentValues cv = new ContentValues();
        cv.put("qr_code", qr);
        cv.put("variety_name", variety);
        cv.put("quantity", qty);
        cv.put("intake_time", time);
        cv.put("farm", farm);

        getWritableDatabase().insertWithOnConflict(
                "inventory",
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    // =====================================================
    // FAST QR CHECK (ONLY THING USED DURING STOCKTAKE)
    // =====================================================

    public boolean qrExists(String qr, String farm) {

        Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM inventory WHERE qr_code=? AND farm=? LIMIT 1",
                new String[]{qr, farm}
        );

        boolean exists = c.moveToFirst();
        c.close();

        return exists;
    }

    // =====================================================
    // STOCKTAKE SCANS
    // =====================================================

    public boolean insertScan(String qr, String farm) {

        SQLiteDatabase db = getWritableDatabase();

        boolean isDuplicate = isDuplicateScan(qr, farm);

        ContentValues cv = new ContentValues();
        cv.put("qr_code", qr);
        cv.put("farm", farm);
        cv.put("is_duplicate", isDuplicate ? 1 : 0);
        cv.put("synced", 0);

        long res = db.insert("scans", null, cv);

        return res != -1;
    }

    public boolean isDuplicateScan(String qr, String farm) {

        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM scans " +
                        "WHERE qr_code=? AND farm=? " +
                        "AND scanned_at >= datetime('now','-30 seconds')",
                new String[]{qr, farm}
        );

        boolean duplicate = false;

        if (c.moveToFirst()) {
            duplicate = c.getInt(0) > 0;
        }

        c.close();
        return duplicate;
    }

    public boolean alreadyScanned(String qr, String farm) {

        Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM scans WHERE qr_code=? AND farm=? LIMIT 1",
                new String[]{qr, farm}
        );

        boolean exists = c.moveToFirst();
        c.close();

        return exists;
    }

    public Cursor getPendingScans() {

        return getReadableDatabase().rawQuery(
                "SELECT qr_code, farm, is_duplicate " +
                        "FROM scans " +
                        "WHERE synced=0",
                null
        );
    }

    public void markAllScansSynced() {
        getWritableDatabase().execSQL(
                "UPDATE scans SET synced=1 WHERE synced=0"
        );
    }

    public int getScanCount(String farm) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM scans WHERE farm=?",
                new String[]{farm}
        );

        int total = 0;
        if (c.moveToFirst()) total = c.getInt(0);
        c.close();


        return total;
    }

    // CLEAR SCANS
    public void clearScans() {

        getWritableDatabase().execSQL(
                "DELETE FROM scans"
        );
    }

    public Cursor getRecentScans() {

        return getReadableDatabase().rawQuery(
                "SELECT qr_code, is_duplicate, synced, scanned_at " +
                        "FROM scans " +
                        "ORDER BY id DESC",
                null
        );
    }



    public int getPendingCount() {

        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM scans WHERE synced=0",
                null
        );

        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();

        return count;
    }

    public int getSyncedCount() {

        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM scans WHERE synced=1",
                null
        );

        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();

        return count;
    }
    public void clearQrMaster() {

        getWritableDatabase().execSQL(
                "DELETE FROM qr_master"
        );
    }

    public void insertQrMaster(
            String serial,
            String qrCode,
            String bucketName,
            String farm,
            int length,
            String varietyName,
            int quantity,
            String batchId,
            String sourceTable
    ) {

        ContentValues cv = new ContentValues();

        cv.put("serial", serial);
        cv.put("qr_code", qrCode);
        cv.put("bucket_name", bucketName);
        cv.put("farm", farm);
        cv.put("length", length);
        cv.put("variety_name", varietyName);
        cv.put("quantity", quantity);
        cv.put("batch_id", batchId);
        cv.put("source_table", sourceTable);

        getWritableDatabase().insertWithOnConflict(
                "qr_master",
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    public void insertVariety(
            int varietyId,
            String varietyName,
            String code,
            String type,
            String colour,
            String flowerTypeId
    ) {

        ContentValues cv = new ContentValues();

        cv.put("variety_id", varietyId);
        cv.put("variety_name", varietyName);
        cv.put("code", code);
        cv.put("type", type);
        cv.put("colour", colour);
        cv.put("flower_type_id", flowerTypeId);

        getWritableDatabase().insertWithOnConflict(
                "summers_varieties",
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    public void insertQrFixHeader(
            String qr,
            String serial,
            String farm,
            int length,
            String bucketName
    ) {

        ContentValues cv = new ContentValues();
        cv.put("qr_code", qr);
        cv.put("serial", serial);
        cv.put("farm", farm);
        cv.put("length", length);
        cv.put("bucket_name", bucketName);

        getWritableDatabase().insertWithOnConflict(
                "qr_fix_header",
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }


    public void insertQrFixItem(
            String qr,
            int varietyId,
            String varietyName,
            int quantity
    ) {

        SQLiteDatabase db = getWritableDatabase();

        Cursor c = db.rawQuery(
                "SELECT quantity FROM qr_fix_items WHERE qr_code=? AND variety_id=?",
                new String[]{qr, String.valueOf(varietyId)}
        );

        if (c.moveToFirst()) {

            int existingQty = c.getInt(0);
            ContentValues cv = new ContentValues();
            cv.put("quantity", existingQty + quantity);

            db.update(
                    "qr_fix_items",
                    cv,
                    "qr_code=? AND variety_id=?",
                    new String[]{qr, String.valueOf(varietyId)}
            );

        } else {

            ContentValues cv = new ContentValues();
            cv.put("qr_code", qr);
            cv.put("variety_id", varietyId);
            cv.put("variety_name", varietyName);
            cv.put("quantity", quantity);
            cv.put("synced", 0);

            db.insert("qr_fix_items", null, cv);
        }

        c.close();
    }

    public Cursor getQrDetailsByBatch(String batchId) {

        return getReadableDatabase().rawQuery(
                "SELECT * FROM qr_master WHERE batch_id=?",
                new String[]{batchId}
        );
    }

    public String getBatchBySerial(String serial) {

        Cursor c = getReadableDatabase().rawQuery(
                "SELECT batch_id FROM qr_master " +
                        "WHERE UPPER(serial)=UPPER(?) " +
                        "LIMIT 1",
                new String[]{serial}
        );

        String batch = null;

        if (c.moveToFirst()) {
            batch = c.getString(0);
        }

        c.close();

        return batch;
    }


    public int getQrMasterCount() {

        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM qr_master",
                null
        );

        int total = 0;

        if(c.moveToFirst())
            total = c.getInt(0);

        c.close();

        return total;
    }



    public Cursor getQrDetailsByQr(String qr) {

        return getReadableDatabase().rawQuery(
                "SELECT * FROM qr_master WHERE qr_code=?",
                new String[]{qr}
        );
    }

    public void markSyncedNonDuplicates() {
        getWritableDatabase().execSQL(
                "UPDATE scans SET synced=1 WHERE synced=0 AND is_duplicate=0"
        );
    }

    public ArrayList<VarietyItem> searchVarieties(String term) {

        ArrayList<VarietyItem> list = new ArrayList<>();

        Cursor c = getReadableDatabase().rawQuery(
                "SELECT variety_id, variety_name " +
                        "FROM summers_varieties " +
                        "WHERE variety_name LIKE ? " +
                        "ORDER BY variety_name " +
                        "LIMIT 20",
                new String[]{"%" + term + "%"}
        );

        while (c.moveToNext()) {

            list.add(
                    new VarietyItem(
                            c.getInt(0),
                            c.getString(1)
                    )
            );
        }

        c.close();

        return list;
    }

    public int getVarietyCount() {

        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM summers_varieties",
                null
        );

        c.moveToFirst();

        int count = c.getInt(0);

        c.close();

        return count;
    }

    public static class VarietyItem {

        public int id;
        public String name;

        public VarietyItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public void insertInventoryFix(
            String batchId,
            String oldVariety,
            int oldQty,
            String newVariety,
            int newQty
    ) {

        ContentValues cv = new ContentValues();

        cv.put("batch_id", batchId);
        cv.put("old_variety", oldVariety);
        cv.put("old_qty", oldQty);
        cv.put("new_variety", newVariety);
        cv.put("new_qty", newQty);
        cv.put("synced", 0);

        getWritableDatabase().insert(
                "inventory_fixes",
                null,
                cv
        );
    }

    public Cursor getPendingInventoryFixes() {

        return getReadableDatabase().rawQuery(
                "SELECT * " +
                        "FROM inventory_fixes " +
                        "WHERE synced=0",
                null
        );
    }

    public void markInventoryFixesSynced() {

        getWritableDatabase().execSQL(
                "UPDATE inventory_fixes " +
                        "SET synced=1 " +
                        "WHERE synced=0"
        );
    }

    public String getBatchByQr(String qr) {

        Cursor c = getReadableDatabase().rawQuery(
                "SELECT batch_id " +
                        "FROM qr_master " +
                        "WHERE qr_code=? " +
                        "LIMIT 1",
                new String[]{qr}
        );

        String batchId = null;

        if (c.moveToFirst()) {
            batchId = c.getString(0);
        }

        c.close();

        return batchId;
    }

}