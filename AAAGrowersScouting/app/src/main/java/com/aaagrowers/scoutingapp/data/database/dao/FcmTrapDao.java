package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.aaagrowers.scoutingapp.data.database.entity.FcmTrapEntity;

import java.util.List;

@Dao
public interface FcmTrapDao {

    /*
     * ============================================================
     * INSERT / UPDATE
     * ============================================================
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(
            List<FcmTrapEntity> traps
    );


    /*
     * ============================================================
     * DELETE ALL
     * ============================================================
     */

    @Query("DELETE FROM fcm_traps")
    void deleteAll();


    /*
     * ============================================================
     * FIND BY QR CODE
     * ============================================================
     *
     * The QR code contains the secure token stored in qr_code.
     */

    @Query(
            "SELECT * FROM fcm_traps " +
                    "WHERE qr_code = :qrCode " +
                    "AND status = 'active' " +
                    "LIMIT 1"
    )
    FcmTrapEntity findByQrCode(
            String qrCode
    );


    /*
     * ============================================================
     * FIND BY TRAP CODE
     * ============================================================
     */

    @Query(
            "SELECT * FROM fcm_traps " +
                    "WHERE trap_code = :trapCode " +
                    "LIMIT 1"
    )
    FcmTrapEntity findByTrapCode(
            String trapCode
    );


    /*
     * ============================================================
     * GET ALL ACTIVE TRAPS
     * ============================================================
     */

    @Query(
            "SELECT * FROM fcm_traps " +
                    "WHERE status = 'active' " +
                    "ORDER BY trap_code ASC"
    )
    List<FcmTrapEntity> getActiveTraps();


    /*
     * ============================================================
     * COUNT
     * ============================================================
     */

    @Query(
            "SELECT COUNT(*) FROM fcm_traps " +
                    "WHERE status = 'active'"
    )
    int getActiveTrapCount();

    @Query(
            "SELECT * FROM fcm_traps " +
                    "WHERE id = :trapId " +
                    "LIMIT 1"
    )
    FcmTrapEntity findByTrapId(int trapId);
}