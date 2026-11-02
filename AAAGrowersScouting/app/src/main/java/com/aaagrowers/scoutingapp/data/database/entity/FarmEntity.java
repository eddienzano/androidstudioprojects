package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "farms")
public class FarmEntity {

    @PrimaryKey
    public int id;

    public String farm_code;

    public String farm_name;

    public String description;

    public int active;

    public String created_at;

    public String updated_at;
}