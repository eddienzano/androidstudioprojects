package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aaagrowers.scoutingapp.data.database.entity.BayEntity;

import java.util.List;

@Dao
public interface BayDao {

    @Insert
    long insert(BayEntity bay);

    @Insert
    void insertAll(List<BayEntity> bays);

    @Update
    int update(BayEntity bay);

    @Query(
            "SELECT * FROM bays " +
                    "ORDER BY bay_number ASC"
    )
    List<BayEntity> getAll();

    @Query(
            "SELECT * FROM bays " +
                    "WHERE id = :id " +
                    "LIMIT 1"
    )
    BayEntity getById(int id);

    @Query(
            "SELECT * FROM bays " +
                    "WHERE greenhouse_id = :greenhouseId " +
                    "ORDER BY bay_number ASC"
    )
    List<BayEntity> getByGreenhouse(int greenhouseId);

    @Query(
            "SELECT * FROM bays " +
                    "WHERE greenhouse_id = :greenhouseId " +
                    "AND active = 1 " +
                    "ORDER BY bay_number ASC"
    )
    List<BayEntity> getActiveByGreenhouse(int greenhouseId);

    @Query("SELECT COUNT(*) FROM bays")
    int count();

    @Query(
            "SELECT COUNT(*) FROM bays " +
                    "WHERE greenhouse_id = :greenhouseId"
    )
    int countByGreenhouse(int greenhouseId);

    @Query("DELETE FROM bays")
    void deleteAll();
}