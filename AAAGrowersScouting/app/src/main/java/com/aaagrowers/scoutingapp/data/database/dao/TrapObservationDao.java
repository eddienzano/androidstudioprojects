package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.aaagrowers.scoutingapp.data.database.entity.TrapObservationEntity;

import java.util.List;

@Dao
public interface TrapObservationDao {


    // ================================================================
    // INSERT
    // ================================================================

    @Insert
    long insert(
            TrapObservationEntity observation
    );


    // ================================================================
    // PENDING OBSERVATIONS
    // ================================================================

    @Query(
            "SELECT * FROM trap_observations " +
                    "WHERE sync_status = 'pending' " +
                    "ORDER BY id ASC"
    )
    List<TrapObservationEntity> getPendingObservations();


    // ================================================================
    // PENDING COUNT
    // ================================================================

    @Query(
            "SELECT COUNT(*) FROM trap_observations " +
                    "WHERE sync_status = 'pending'"
    )
    int getPendingCount();


    // ================================================================
    // OBSERVATION COUNT FOR TRAP
    // ================================================================

    @Query(
            "SELECT COUNT(*) FROM trap_observations " +
                    "WHERE trap_id = :trapId"
    )
    int getObservationCountForTrap(
            int trapId
    );


    // ================================================================
    // FIND BY ID
    // ================================================================

    @Query(
            "SELECT * FROM trap_observations " +
                    "WHERE id = :id " +
                    "LIMIT 1"
    )
    TrapObservationEntity findById(
            long id
    );


    // ================================================================
    // UPDATE SYNC STATUS
    // ================================================================

    @Query(
            "UPDATE trap_observations " +
                    "SET sync_status = :status, " +
                    "sync_error = :error " +
                    "WHERE id = :id"
    )
    void updateSyncStatus(
            long id,
            String status,
            String error
    );


    // ================================================================
    // DELETE SYNCHRONIZED OBSERVATION
    // ================================================================

    @Query(
            "DELETE FROM trap_observations " +
                    "WHERE id = :id"
    )
    void deleteById(
            long id
    );


    // ================================================================
    // ALL OBSERVATIONS
    // ================================================================

    @Query(
            "SELECT * FROM trap_observations " +
                    "ORDER BY id DESC"
    )
    List<TrapObservationEntity> getAllObservations();


    // ================================================================
    // FAILED OBSERVATIONS
    // ================================================================

    @Query(
            "SELECT * FROM trap_observations " +
                    "WHERE sync_status = 'failed' " +
                    "ORDER BY id ASC"
    )
    List<TrapObservationEntity> getFailedObservations();


    // ================================================================
    // FAILED COUNT
    // ================================================================

    @Query(
            "SELECT COUNT(*) FROM trap_observations " +
                    "WHERE sync_status = 'failed'"
    )
    int getFailedCount();
}