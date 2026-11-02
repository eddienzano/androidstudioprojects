package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bays")
public class BayEntity {

    @PrimaryKey
    public int id;

    public int greenhouse_id;

    public int bay_number;

    public String bay_name;

    public int active;

    public String created_at;

    public String updated_at;
}