package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aaagrowers.scoutingapp.data.database.entity.ScoutingSeverePointEntity;

import java.util.List;

@Dao
public interface ScoutingSeverePointDao {

    // ============================================================
    // INSERT / UPDATE
    // ============================================================

    @Insert
    long insert(
            ScoutingSeverePointEntity severePoint
    );

    @Insert
    void insertAll(
            List<ScoutingSeverePointEntity> severePoints
    );

    @Update
    void update(
            ScoutingSeverePointEntity severePoint
    );


    // ============================================================
    // GENERAL QUERIES
    // ============================================================

    @Query(
            "SELECT * FROM scouting_severe_points " +
                    "ORDER BY observed_at DESC"
    )
    List<ScoutingSeverePointEntity> getAll();

    @Query(
            "SELECT * FROM scouting_severe_points " +
                    "WHERE id = :id " +
                    "LIMIT 1"
    )
    ScoutingSeverePointEntity getById(
            long id
    );

    @Query(
            "SELECT * FROM scouting_severe_points " +
                    "WHERE client_uuid = :clientUuid " +
                    "LIMIT 1"
    )
    ScoutingSeverePointEntity getByClientUuid(
            String clientUuid
    );


    // ============================================================
    // ROUND
    // ============================================================

    @Query(
            "SELECT * FROM scouting_severe_points " +
                    "WHERE round_id = :roundId " +
                    "ORDER BY observed_at ASC"
    )
    List<ScoutingSeverePointEntity> getByRound(
            long roundId
    );


    // ============================================================
    // SCANNING POINT
    // ============================================================

    @Query(
            "SELECT * FROM scouting_severe_points " +
                    "WHERE scanning_point_id = :scanningPointId " +
                    "ORDER BY observed_at DESC"
    )
    List<ScoutingSeverePointEntity> getByScanningPoint(
            int scanningPointId
    );


    // ============================================================
    // BED
    // ============================================================

    @Query(
            "SELECT * FROM scouting_severe_points " +
                    "WHERE bed_id = :bedId " +
                    "ORDER BY observed_at DESC"
    )
    List<ScoutingSeverePointEntity> getByBed(
            int bedId
    );


    // ============================================================
    // ROUND + BED
    // ============================================================

    @Query(
            "SELECT * FROM scouting_severe_points " +
                    "WHERE round_id = :roundId " +
                    "AND bed_id = :bedId " +
                    "ORDER BY observed_at ASC"
    )
    List<ScoutingSeverePointEntity> getByRoundAndBed(
            long roundId,
            int bedId
    );


    // ============================================================
    // ROUND + OBSERVATION TYPE
    // ============================================================

    @Query(
            "SELECT * FROM scouting_severe_points " +
                    "WHERE round_id = :roundId " +
                    "AND observation_type_id = :observationTypeId " +
                    "ORDER BY observed_at ASC"
    )
    List<ScoutingSeverePointEntity> getByRoundAndObservationType(
            long roundId,
            int observationTypeId
    );


    // ============================================================
    // SEVERITY
    // ============================================================

    @Query(
            "SELECT * FROM scouting_severe_points " +
                    "WHERE round_id = :roundId " +
                    "AND score >= :minimumScore " +
                    "ORDER BY score DESC, observed_at ASC"
    )
    List<ScoutingSeverePointEntity> getSevereByMinimumScore(
            long roundId,
            int minimumScore
    );


    // ============================================================
    // SYNC
    // ============================================================

    @Query(
            "SELECT * FROM scouting_severe_points " +
                    "WHERE sync_status IS NULL " +
                    "OR sync_status = 'pending' " +
                    "OR sync_status = 'failed' " +
                    "ORDER BY created_at ASC"
    )
    List<ScoutingSeverePointEntity> getPendingSync();

    @Query(
            "SELECT * FROM scouting_severe_points " +
                    "WHERE sync_status = 'failed' " +
                    "ORDER BY created_at ASC"
    )
    List<ScoutingSeverePointEntity> getFailedSync();


    // ============================================================
    // SYNC STATUS
    // ============================================================

    @Query(
            "UPDATE scouting_severe_points " +
                    "SET sync_status = 'syncing', sync_error = NULL " +
                    "WHERE id = :id"
    )
    void markSyncing(
            long id
    );

    @Query(
            "UPDATE scouting_severe_points " +
                    "SET sync_status = 'synced', sync_error = NULL " +
                    "WHERE id = :id"
    )
    void markSynced(
            long id
    );

    @Query(
            "UPDATE scouting_severe_points " +
                    "SET sync_status = 'failed', sync_error = :error " +
                    "WHERE id = :id"
    )
    void markFailed(
            long id,
            String error
    );

    @Query(
            "UPDATE scouting_severe_points " +
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
            "SELECT COUNT(*) FROM scouting_severe_points"
    )
    int count();

    @Query(
            "SELECT COUNT(*) FROM scouting_severe_points " +
                    "WHERE round_id = :roundId"
    )
    int countByRound(
            long roundId
    );

    @Query(
            "SELECT COUNT(*) FROM scouting_severe_points " +
                    "WHERE sync_status IS NULL " +
                    "OR sync_status = 'pending' " +
                    "OR sync_status = 'failed'"
    )
    int countPendingSync();

    @Query(
            "SELECT COUNT(*) FROM scouting_severe_points " +
                    "WHERE sync_status = 'failed'"
    )
    int countFailedSync();


    // ============================================================
    // DELETE
    // ============================================================

    @Query(
            "DELETE FROM scouting_severe_points"
    )
    void deleteAll();

    @Query(
            "DELETE FROM scouting_severe_points " +
                    "WHERE round_id = :roundId"
    )
    void deleteByRound(
            long roundId
    );
    @Query(
            "UPDATE scouting_severe_points " +
                    "SET sync_status = 'pending', sync_error = NULL " +
                    "WHERE sync_status = 'syncing'"
    )
    void resetSyncingToPending();
}