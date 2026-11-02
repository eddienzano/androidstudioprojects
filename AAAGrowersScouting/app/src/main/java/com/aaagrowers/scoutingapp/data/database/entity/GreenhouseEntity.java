package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "greenhouses")
public class GreenhouseEntity {

    @PrimaryKey
    public int id;

    public int section_id;

    public String greenhouse_code;

    public String greenhouse_name;

    public String description;

    public int active;

    public String created_at;

    public String updated_at;
}