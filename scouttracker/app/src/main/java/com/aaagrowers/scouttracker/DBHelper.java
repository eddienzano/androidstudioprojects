package com.aaagrowers.scouttracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "scout_tracking.db";
    private static final int DB_VERSION = 3;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE location_trace (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "session_id TEXT," +
                "latitude REAL," +
                "longitude REAL," +
                "accuracy REAL," +
                "timestamp TEXT)");

        db.execSQL("CREATE TABLE observations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "session_id TEXT," +
                "latitude REAL," +
                "longitude REAL," +
                "start_time TEXT," +
                "end_time TEXT," +
                "duration INTEGER," +
                "greenhouse TEXT," +
                "bay TEXT," +
                "issues TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS location_trace");
        db.execSQL("DROP TABLE IF EXISTS observations");
        onCreate(db);
    }

    public void insertLocation(double lat, double lng, float accuracy, String timestamp, String sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("latitude", lat);
        values.put("longitude", lng);
        values.put("accuracy", accuracy);
        values.put("timestamp", timestamp);
        values.put("session_id", sessionId);

        db.insert("location_trace", null, values);
    }

    public void insertObservation(double lat, double lng,
                                  String startTime,
                                  String endTime,
                                  long duration,
                                  String greenhouse,
                                  String bay,
                                  String issuesJson, String sessionId) {



        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("latitude", lat);
        values.put("longitude", lng);
        values.put("start_time", startTime);
        values.put("end_time", endTime);
        values.put("duration", duration);
        values.put("greenhouse", greenhouse);
        values.put("bay", bay);
        values.put("issues", issuesJson);
        values.put("session_id", sessionId);

        db.insert("observations", null, values);
    }
}