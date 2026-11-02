package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aaagrowers.scoutingapp.data.database.entity.ScoutingObservationEntity;

import java.util.List;

@Dao
public interface ScoutingObservationDao {

    // ============================================================
    // INSERT / UPDATE
    // ============================================================

    @Insert
    long insert(
            ScoutingObservationEntity observation
    );

    @Insert
    void insertAll(
            List<ScoutingObservationEntity> observations
    );

    @Update
    void update(
            ScoutingObservationEntity observation
    );


    // ============================================================
    // GENERAL QUERIES
    // ============================================================

    @Query(
            "SELECT * FROM scouting_observations " +
                    "ORDER BY observed_at DESC"
    )
    List<ScoutingObservationEntity> getAll();

    @Query(
            "SELECT * FROM scouting_observations " +
                    "WHERE id = :id " +
                    "LIMIT 1"
    )
    ScoutingObservationEntity getById(
            long id
    );

    @Query(
            "SELECT * FROM scouting_observations " +
                    "WHERE client_uuid = :clientUuid " +
                    "LIMIT 1"
    )
    ScoutingObservationEntity getByClientUuid(
            String clientUuid
    );


    // ============================================================
    // ROUND
    // ============================================================

    @Query(
            "SELECT * FROM scouting_observations " +
                    "WHERE round_id = :roundId " +
                    "ORDER BY observed_at ASC"
    )
    List<ScoutingObservationEntity> getByRound(
            long roundId
    );


    // ============================================================
    // SCANNING POINT
    // ============================================================

    @Query(
            "SELECT * FROM scouting_observations " +
                    "WHERE scanning_point_id = :scanningPointId " +
                    "ORDER BY observed_at DESC"
    )
    List<ScoutingObservationEntity> getByScanningPoint(
            int scanningPointId
    );


    // ============================================================
    // ROUND + SCANNING POINT
    // ============================================================

    @Query(
            "SELECT * FROM scouting_observations " +
                    "WHERE round_id = :roundId " +
                    "AND scanning_point_id = :scanningPointId " +
                    "ORDER BY observed_at ASC"
    )
    List<ScoutingObservationEntity> getByRoundAndScanningPoint(
            long roundId,
            int scanningPointId
    );


    // ============================================================
    // ROUND + OBSERVATION TYPE
    // ============================================================

    @Query(
            "SELECT * FROM scouting_observations " +
                    "WHERE round_id = :roundId " +
                    "AND observation_type_id = :observationTypeId " +
                    "ORDER BY observed_at ASC"
    )
    List<ScoutingObservationEntity> getByRoundAndObservationType(
            long roundId,
            int observationTypeId
    );


    // ============================================================
    // SYNC QUERIES
    // ============================================================

    @Query(
            "SELECT * FROM scouting_observations " +
                    "WHERE sync_status IS NULL " +
                    "OR sync_status = 'pending' " +
                    "OR sync_status = 'failed' " +
                    "ORDER BY created_at ASC"
    )
    List<ScoutingObservationEntity> getPendingSync();

    @Query(
            "SELECT * FROM scouting_observations " +
                    "WHERE sync_status = 'failed' " +
                    "ORDER BY created_at ASC"
    )
    List<ScoutingObservationEntity> getFailedSync();


    // ============================================================
    // SYNC STATUS
    // ============================================================

    @Query(
            "UPDATE scouting_observations " +
                    "SET sync_status = 'syncing', sync_error = NULL " +
                    "WHERE id = :id"
    )
    void markSyncing(
            long id
    );

    @Query(
            "UPDATE scouting_observations " +
                    "SET sync_status = 'synced', sync_error = NULL " +
                    "WHERE id = :id"
    )
    void markSynced(
            long id
    );

    @Query(
            "UPDATE scouting_observations " +
                    "SET sync_status = 'failed', sync_error = :error " +
                    "WHERE id = :id"
    )
    void markFailed(
            long id,
            String error
    );

    @Query(
            "UPDATE scouting_observations " +
                    "SET sync_status = 'pending', sync_error = NULL " +
                    "WHERE id = :id"
    )
    void markPending(
            long id
    );


    // ============================================================
    // COUNTS
    // ============================================================

    @Query(
            "SELECT COUNT(*) FROM scouting_observations"
    )
    int count();

    @Query(
            "SELECT COUNT(*) FROM scouting_observations " +
                    "WHERE round_id = :roundId"
    )
    int countByRound(
            long roundId
    );

    @Query(
            "SELECT COUNT(*) FROM scouting_observations " +
                    "WHERE sync_status IS NULL " +
                    "OR sync_status = 'pending' " +
                    "OR sync_status = 'failed'"
    )
    int countPendingSync();

    @Query(
            "SELECT COUNT(*) FROM scouting_observations " +
                    "WHERE sync_status = 'failed'"
    )
    int countFailedSync();


    // ============================================================
    // DELETE
    // ============================================================

    @Query(
            "DELETE FROM scouting_observations"
    )
    void deleteAll();

    @Query(
            "DELETE FROM scouting_observations " +
                    "WHERE round_id = :roundId"
    )
    void deleteByRound(
            long roundId
    );

    @Query(
            "UPDATE scouting_observations " +
                    "SET sync_status = 'pending', sync_error = NULL " +
                    "WHERE sync_status = 'syncing'"
    )
    void resetSyncingToPending();
}