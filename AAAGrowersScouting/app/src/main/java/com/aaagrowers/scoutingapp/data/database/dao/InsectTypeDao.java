package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.aaagrowers.scoutingapp.data.database.entity.InsectTypeEntity;

import java.util.List;

@Dao
public interface InsectTypeDao {

    /*
     * ============================================================
     * INSERT / UPDATE
     * ============================================================
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(
            List<InsectTypeEntity> insectTypes
    );


    /*
     * ============================================================
     * DELETE ALL
     * ============================================================
     */

    @Query("DELETE FROM insect_types")
    void deleteAll();


    /*
     * ============================================================
     * FIND BY ID
     * ============================================================
     */

    @Query(
            "SELECT * FROM insect_types " +
                    "WHERE id = :id " +
                    "LIMIT 1"
    )
    InsectTypeEntity findById(
            int id
    );


    /*
     * ============================================================
     * FIND BY CODE
     * ============================================================
     */

    @Query(
            "SELECT * FROM insect_types " +
                    "WHERE code = :code " +
                    "LIMIT 1"
    )
    InsectTypeEntity findByCode(
            String code
    );


    /*
     * ============================================================
     * GET ALL ACTIVE INSECT TYPES
     * ============================================================
     */

    @Query(
            "SELECT * FROM insect_types " +
                    "WHERE active = 1 " +
                    "ORDER BY sort_order ASC, name ASC"
    )
    List<InsectTypeEntity> getActiveInsectTypes();


    /*
     * ============================================================
     * COUNT
     * ============================================================
     */

    @Query(
            "SELECT COUNT(*) FROM insect_types " +
                    "WHERE active = 1"
    )
    int getActiveInsectTypeCount();
}