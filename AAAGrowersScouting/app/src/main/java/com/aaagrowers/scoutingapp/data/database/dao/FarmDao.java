package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aaagrowers.scoutingapp.data.database.entity.FarmEntity;

import java.util.List;

@Dao
public interface FarmDao {

    @Insert
    long insert(FarmEntity farm);

    @Insert
    void insertAll(List<FarmEntity> farms);


    @Update
    int update(FarmEntity farm);

    @Query("SELECT * FROM farms ORDER BY farm_name ASC")
    List<FarmEntity> getAll();

    @Query("SELECT * FROM farms WHERE id = :id LIMIT 1")
    FarmEntity getById(int id);

    @Query(
            "SELECT * FROM farms " +
                    "WHERE active = 1 " +
                    "ORDER BY farm_name ASC"
    )
    List<FarmEntity> getActive();

    @Query("SELECT COUNT(*) FROM farms")
    int count();

    @Query("SELECT COUNT(*) FROM farms WHERE active = 1")
    int countActive();

    @Query("DELETE FROM farms")
    void deleteAll();
}