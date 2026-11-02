package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "scouting_observations")
public class ScoutingObservationEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String client_uuid;

    public long round_id;

    public int scanning_point_id;

    public int greenhouse_variety_id;

    public int observation_type_id;

    public int score;

    public String trend;

    public String comment;

    public String photo_path;

    public String observed_at;

    public String created_at;

    public String updated_at;

    // ============================================================
    // SYNC
    // ============================================================

    public String sync_status;

    public String sync_error;
}