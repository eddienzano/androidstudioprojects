package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aaagrowers.scoutingapp.data.database.entity.BedEntity;

import java.util.List;

@Dao
public interface BedDao {

    @Insert
    long insert(BedEntity bed);

    @Insert
    void insertAll(List<BedEntity> beds);

    @Update
    int update(BedEntity bed);

    @Query(
            "SELECT * FROM beds " +
                    "ORDER BY bed_number ASC"
    )
    List<BedEntity> getAll();

    @Query(
            "SELECT * FROM beds " +
                    "WHERE id = :id " +
                    "LIMIT 1"
    )
    BedEntity getById(int id);

    @Query(
            "SELECT * FROM beds " +
                    "WHERE bay_id = :bayId " +
                    "ORDER BY bed_number ASC"
    )
    List<BedEntity> getByBay(int bayId);

    @Query(
            "SELECT * FROM beds " +
                    "WHERE bay_id = :bayId " +
                    "AND active = 1 " +
                    "ORDER BY bed_number ASC"
    )
    List<BedEntity> getActiveByBay(int bayId);

    @Query(
            "SELECT * FROM beds " +
                    "WHERE bay_id = :bayId " +
                    "AND side = :side " +
                    "ORDER BY bed_number ASC"
    )
    List<BedEntity> getByBayAndSide(
            int bayId,
            String side
    );

    @Query("SELECT COUNT(*) FROM beds")
    int count();

    @Query(
            "SELECT COUNT(*) FROM beds " +
                    "WHERE bay_id = :bayId"
    )
    int countByBay(int bayId);

    @Query("DELETE FROM beds")
    void deleteAll();
}