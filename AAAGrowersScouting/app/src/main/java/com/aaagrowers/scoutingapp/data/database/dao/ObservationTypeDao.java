package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aaagrowers.scoutingapp.data.database.entity.ObservationTypeEntity;

import java.util.List;

@Dao
public interface ObservationTypeDao {

    @Insert
    long insert(
            ObservationTypeEntity observationType
    );

    @Insert
    void insertAll(
            List<ObservationTypeEntity> observationTypes
    );

    @Update
    void update(
            ObservationTypeEntity observationType
    );

    @Query(
            "SELECT * FROM observation_types " +
                    "ORDER BY sort_order ASC, observation_name ASC"
    )
    List<ObservationTypeEntity> getAll();

    @Query(
            "SELECT * FROM observation_types " +
                    "WHERE active = 1 " +
                    "ORDER BY sort_order ASC, observation_name ASC"
    )
    List<ObservationTypeEntity> getActive();

    @Query(
            "SELECT * FROM observation_types " +
                    "WHERE id = :id " +
                    "LIMIT 1"
    )
    ObservationTypeEntity getById(int id);

    @Query(
            "SELECT * FROM observation_types " +
                    "WHERE observation_code = :code " +
                    "LIMIT 1"
    )
    ObservationTypeEntity getByCode(String code);

    @Query(
            "SELECT * FROM observation_types " +
                    "WHERE category = :category " +
                    "AND active = 1 " +
                    "ORDER BY sort_order ASC, observation_name ASC"
    )
    List<ObservationTypeEntity> getActiveByCategory(
            String category
    );

    @Query(
            "SELECT COUNT(*) FROM observation_types"
    )
    int count();

    @Query(
            "SELECT COUNT(*) FROM observation_types " +
                    "WHERE active = 1"
    )
    int countActive();

    @Query(
            "DELETE FROM observation_types"
    )
    void deleteAll();
}