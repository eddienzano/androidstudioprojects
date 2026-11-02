package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aaagrowers.scoutingapp.data.database.entity.ScoutingRoundEntity;

import java.util.List;

@Dao
public interface ScoutingRoundDao {

    // ============================================================
    // INSERT / UPDATE
    // ============================================================

    @Insert
    long insert(
            ScoutingRoundEntity round
    );

    @Update
    void update(
            ScoutingRoundEntity round
    );


    // ============================================================
    // GENERAL QUERIES
    // ============================================================

    @Query(
            "SELECT * FROM scouting_rounds " +
                    "ORDER BY round_date DESC, started_at DESC"
    )
    List<ScoutingRoundEntity> getAll();

    @Query(
            "SELECT * FROM scouting_rounds " +
                    "WHERE id = :id " +
                    "LIMIT 1"
    )
    ScoutingRoundEntity getById(
            long id
    );

    @Query(
            "SELECT * FROM scouting_rounds " +
                    "WHERE client_uuid = :clientUuid " +
                    "LIMIT 1"
    )
    ScoutingRoundEntity getByClientUuid(
            String clientUuid
    );

    @Query(
            "SELECT * FROM scouting_rounds " +
                    "WHERE scout_id = :scoutId " +
                    "ORDER BY round_date DESC, started_at DESC"
    )
    List<ScoutingRoundEntity> getByScout(
            int scoutId
    );


    // ============================================================
    // OPEN ROUNDS
    // ============================================================

    @Query(
            "SELECT * FROM scouting_rounds " +
                    "WHERE status = 'open' " +
                    "ORDER BY started_at DESC"
    )
    List<ScoutingRoundEntity> getOpenRounds();

    @Query(
            "SELECT * FROM scouting_rounds " +
                    "WHERE scout_id = :scoutId " +
                    "AND status = 'open' " +
                    "ORDER BY started_at DESC"
    )
    List<ScoutingRoundEntity> getOpenRoundsForScout(
            int scoutId
    );


    // ============================================================
    // SYNC QUERIES
    // ============================================================

    @Query(
            "SELECT * FROM scouting_rounds " +
                    "WHERE sync_status IS NULL " +
                    "OR sync_status = 'pending' " +
                    "OR sync_status = 'failed' " +
                    "ORDER BY created_at ASC"
    )
    List<ScoutingRoundEntity> getPendingSync();

    @Query(
            "SELECT * FROM scouting_rounds " +
                    "WHERE sync_status = 'failed' " +
                    "ORDER BY created_at ASC"
    )
    List<ScoutingRoundEntity> getFailedSync();

    @Query(
            "SELECT * FROM scouting_rounds " +
                    "WHERE sync_status = 'syncing' " +
                    "ORDER BY created_at ASC"
    )
    List<ScoutingRoundEntity> getSyncing();


    // ============================================================
    // SYNC STATUS
    // ============================================================

    @Query(
            "UPDATE scouting_rounds " +
                    "SET sync_status = 'syncing', sync_error = NULL " +
                    "WHERE id = :id"
    )
    void markSyncing(
            long id
    );

    @Query(
            "UPDATE scouting_rounds " +
                    "SET sync_status = 'synced', sync_error = NULL " +
                    "WHERE id = :id"
    )
    void markSynced(
            long id
    );

    @Query(
            "UPDATE scouting_rounds " +
                    "SET sync_status = 'failed', sync_error = :error " +
                    "WHERE id = :id"
    )
    void markFailed(
            long id,
            String error
    );

    @Query(
            "UPDATE scouting_rounds " +
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
            "SELECT COUNT(*) FROM scouting_rounds"
    )
    int count();

    @Query(
            "SELECT COUNT(*) FROM scouting_rounds " +
                    "WHERE status = 'open'"
    )
    int countOpen();

    @Query(
            "SELECT COUNT(*) FROM scouting_rounds " +
                    "WHERE sync_status IS NULL " +
                    "OR sync_status = 'pending' " +
                    "OR sync_status = 'failed'"
    )
    int countPendingSync();

    @Query(
            "SELECT COUNT(*) FROM scouting_rounds " +
                    "WHERE sync_status = 'failed'"
    )
    int countFailedSync();


    // ============================================================
    // DELETE
    // ============================================================

    @Query(
            "DELETE FROM scouting_rounds"
    )
    void deleteAll();

    @Query(
            "UPDATE scouting_rounds " +
                    "SET sync_status = 'pending', sync_error = NULL " +
                    "WHERE sync_status = 'syncing'"
    )
    void resetSyncingToPending();
}