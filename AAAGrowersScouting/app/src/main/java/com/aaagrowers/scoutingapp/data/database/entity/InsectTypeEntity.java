package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "insect_types",
        indices = {

                @Index(
                        value = {"code"},
                        unique = true
                ),

                @Index(
                        value = {"active"}
                ),

                @Index(
                        value = {"sort_order"}
                )
        }
)
public class InsectTypeEntity {

    @PrimaryKey
    public int id;

    public String code;

    public String name;

    public String scientific_name;

    public String description;

    public int active;

    public int sort_order;

    public String created_at;

    public String updated_at;
}