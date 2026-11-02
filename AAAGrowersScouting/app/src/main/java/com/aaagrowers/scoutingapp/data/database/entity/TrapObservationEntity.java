package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "trap_observations",
        indices = {
                @Index(value = {"trap_id"}),
                @Index(value = {"client_uuid"}, unique = true),
                @Index(value = {"sync_status"}),
                @Index(value = {"observed_at"})
        }
)
public class TrapObservationEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /**
     * Unique identifier generated on the device.
     * Used to prevent duplicate observations when syncing.
     */
    @NonNull
    public String client_uuid = "";

    public int trap_id;

    public String trap_code;

    public String trap_type;

    public Integer insect_type_id;

    public String insect_type_name;

    public int moth_count;

    public String trend;

    public String comments;

    public String photo_path;

    public Double latitude;

    public Double longitude;

    public Double gps_accuracy;

    public String observed_at;

    public int user_id;

    @NonNull
    @ColumnInfo(defaultValue = "pending")
    public String sync_status = "pending";

    public String sync_error;

    public String created_at;
}