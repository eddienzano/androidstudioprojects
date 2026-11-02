package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "observation_types")
public class ObservationTypeEntity {

    @PrimaryKey
    public int id;

    public String observation_code;

    public String observation_name;

    public String category;

    public String description;

    public int requires_score;

    public int requires_trend;

    public int allows_comment;

    public int allows_photo;

    public int active;

    public int sort_order;

    public String created_at;

    public String updated_at;
}