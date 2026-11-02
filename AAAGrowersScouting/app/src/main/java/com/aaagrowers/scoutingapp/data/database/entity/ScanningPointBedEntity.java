package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(
        tableName = "scanning_point_beds",
        primaryKeys = {
                "scanning_point_id",
                "bed_id"
        }
)
public class ScanningPointBedEntity {

    public int scanning_point_id;

    public int bed_id;
}