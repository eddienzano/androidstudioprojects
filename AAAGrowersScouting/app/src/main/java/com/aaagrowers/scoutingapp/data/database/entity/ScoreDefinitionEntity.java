package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "score_definitions")
public class ScoreDefinitionEntity {

    @PrimaryKey
    public int id;

    public int score;

    public String score_name;

    public String description;

    public String severity;

    public int active;

    public String created_at;

    public String updated_at;
}