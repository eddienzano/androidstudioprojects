package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aaagrowers.scoutingapp.data.database.entity.UserEntity;

import java.util.List;

@Dao
public interface UserDao {

    @Query("SELECT * FROM users ORDER BY full_name ASC")
    List<UserEntity> getAll();

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    UserEntity getById(int id);

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    UserEntity getByUsername(String username);

    @Query(
            "SELECT * FROM users " +
                    "WHERE username = :username " +
                    "AND active = 1 " +
                    "LIMIT 1"
    )
    UserEntity getActiveByUsername(String username);

    @Insert
    long insert(UserEntity user);

    @Update
    int update(UserEntity user);

    @Query("DELETE FROM users WHERE id = :id")
    int deleteById(int id);

    @Query("SELECT COUNT(*) FROM users")
    int count();

    @Query("SELECT COUNT(*) FROM users WHERE active = 1")
    int countActive();
}