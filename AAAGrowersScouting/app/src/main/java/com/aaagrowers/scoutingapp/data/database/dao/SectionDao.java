package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aaagrowers.scoutingapp.data.database.entity.SectionEntity;

import java.util.List;

@Dao
public interface SectionDao {

    @Insert
    long insert(SectionEntity section);

    @Insert
    void insertAll(List<SectionEntity> sections);

    @Update
    int update(SectionEntity section);

    @Query(
            "SELECT * FROM sections " +
                    "ORDER BY section_name ASC"
    )
    List<SectionEntity> getAll();

    @Query(
            "SELECT * FROM sections " +
                    "WHERE id = :id " +
                    "LIMIT 1"
    )
    SectionEntity getById(int id);

    @Query(
            "SELECT * FROM sections " +
                    "WHERE farm_id = :farmId " +
                    "ORDER BY section_name ASC"
    )
    List<SectionEntity> getByFarm(int farmId);

    @Query(
            "SELECT * FROM sections " +
                    "WHERE farm_id = :farmId " +
                    "AND active = 1 " +
                    "ORDER BY section_name ASC"
    )
    List<SectionEntity> getActiveByFarm(int farmId);

    @Query("SELECT COUNT(*) FROM sections")
    int count();

    @Query(
            "SELECT COUNT(*) FROM sections " +
                    "WHERE farm_id = :farmId"
    )
    int countByFarm(int farmId);

    @Query("DELETE FROM sections")
    void deleteAll();
}