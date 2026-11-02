package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "scouting_rounds")
public class ScoutingRoundEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String client_uuid;

    public int scout_id;

    public int section_id;

    public String round_date;

    public String started_at;

    public String completed_at;

    public String status;

    public String notes;

    public String created_at;

    public String updated_at;

    // ============================================================
    // SYNC
    // ============================================================

    public String sync_status;

    public String sync_error;
}