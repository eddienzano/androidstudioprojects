package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sections")
public class SectionEntity {

    @PrimaryKey
    public int id;

    public int farm_id;

    public String section_code;

    public String section_name;

    public String description;

    public int active;

    public String created_at;

    public String updated_at;
}