package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "fcm_traps")
public class FcmTrapEntity {

    @PrimaryKey
    public int id;

    public String trap_code;

    public String qr_code;

    public int bay_id;

    public String trap_type;

    public Double latitude;

    public Double longitude;

    public String installed_at;

    public String status;

    public String notes;

    public String created_at;

    public String updated_at;
}