package com.aaagrowers.scoutingapp.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    public int id;

    public String username;

    public String full_name;

    public String email;

    public String password_hash;

    public String role;

    public int active;

    public String last_login_at;

    public String created_at;

    public String updated_at;
}