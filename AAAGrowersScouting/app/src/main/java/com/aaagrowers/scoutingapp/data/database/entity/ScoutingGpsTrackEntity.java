package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "scouting_gps_tracks")
public class ScoutingGpsTrackEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String client_uuid;

    public long round_id;

    public double latitude;

    public double longitude;

    public double accuracy;

    public double altitude;

    public double speed;

    public double bearing;

    public String recorded_at;

    public String created_at;

    // ============================================================
    // SYNC
    // ============================================================

    public String sync_status;

    public String sync_error;
}