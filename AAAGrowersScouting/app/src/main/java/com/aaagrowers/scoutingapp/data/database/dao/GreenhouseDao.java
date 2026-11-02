package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aaagrowers.scoutingapp.data.database.entity.GreenhouseEntity;

import java.util.List;

@Dao
public interface GreenhouseDao {

    @Insert
    long insert(GreenhouseEntity greenhouse);

    @Insert
    void insertAll(List<GreenhouseEntity> greenhouses);

    @Update
    int update(GreenhouseEntity greenhouse);

    @Query(
            "SELECT * FROM greenhouses " +
                    "ORDER BY greenhouse_name ASC"
    )
    List<GreenhouseEntity> getAll();

    @Query(
            "SELECT * FROM greenhouses " +
                    "WHERE id = :id " +
                    "LIMIT 1"
    )
    GreenhouseEntity getById(int id);

    @Query(
            "SELECT * FROM greenhouses " +
                    "WHERE section_id = :sectionId " +
                    "ORDER BY greenhouse_name ASC"
    )
    List<GreenhouseEntity> getBySection(int sectionId);

    @Query(
            "SELECT * FROM greenhouses " +
                    "WHERE section_id = :sectionId " +
                    "AND active = 1 " +
                    "ORDER BY greenhouse_name ASC"
    )
    List<GreenhouseEntity> getActiveBySection(int sectionId);

    @Query("SELECT COUNT(*) FROM greenhouses")
    int count();

    @Query("SELECT COUNT(*) FROM greenhouses WHERE active = 1")
    int countActive();

    @Query("DELETE FROM greenhouses")
    void deleteAll();
}