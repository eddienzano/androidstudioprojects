package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "scanning_points")
public class ScanningPointEntity {

    @PrimaryKey
    public int id;

    public int greenhouse_id;

    public int bay_id;

    public String side;

    public String point_code;

    public String point_name;

    public String qr_token;

    public String description;

    public int active;

    public String created_at;

    public String updated_at;
}