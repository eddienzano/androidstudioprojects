package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aaagrowers.scoutingapp.data.database.entity.ScanningPointEntity;

import java.util.List;

@Dao
public interface ScanningPointDao {

    @Insert
    long insert(ScanningPointEntity scanningPoint);

    @Insert
    void insertAll(List<ScanningPointEntity> scanningPoints);

    @Update
    int update(ScanningPointEntity scanningPoint);

    @Query(
            "SELECT * FROM scanning_points " +
                    "ORDER BY point_name ASC"
    )
    List<ScanningPointEntity> getAll();

    @Query(
            "SELECT * FROM scanning_points " +
                    "WHERE id = :id " +
                    "LIMIT 1"
    )
    ScanningPointEntity getById(int id);

    @Query(
            "SELECT * FROM scanning_points " +
                    "WHERE qr_token = :qrToken " +
                    "LIMIT 1"
    )
    ScanningPointEntity getByQrToken(String qrToken);

    @Query(
            "SELECT * FROM scanning_points " +
                    "WHERE greenhouse_id = :greenhouseId " +
                    "ORDER BY point_name ASC"
    )
    List<ScanningPointEntity> getByGreenhouse(
            int greenhouseId
    );

    @Query(
            "SELECT * FROM scanning_points " +
                    "WHERE bay_id = :bayId " +
                    "ORDER BY point_name ASC"
    )
    List<ScanningPointEntity> getByBay(
            int bayId
    );

    @Query(
            "SELECT * FROM scanning_points " +
                    "WHERE bay_id = :bayId " +
                    "AND side = :side " +
                    "ORDER BY point_name ASC"
    )
    List<ScanningPointEntity> getByBayAndSide(
            int bayId,
            String side
    );

    @Query(
            "SELECT * FROM scanning_points " +
                    "WHERE active = 1 " +
                    "ORDER BY point_name ASC"
    )
    List<ScanningPointEntity> getActive();

    @Query("SELECT COUNT(*) FROM scanning_points")
    int count();

    @Query(
            "SELECT COUNT(*) FROM scanning_points " +
                    "WHERE active = 1"
    )
    int countActive();

    @Query("DELETE FROM scanning_points")
    void deleteAll();
}