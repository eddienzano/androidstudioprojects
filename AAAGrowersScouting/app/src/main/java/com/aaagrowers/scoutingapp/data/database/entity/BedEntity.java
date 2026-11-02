package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "beds")
public class BedEntity {

    @PrimaryKey
    public int id;

    public int bay_id;

    public int bed_number;

    public String bed_name;

    public String side;

    public int active;

    public String created_at;

    public String updated_at;
}