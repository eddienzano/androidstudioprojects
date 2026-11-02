package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.aaagrowers.scoutingapp.data.database.entity.ScoutingGpsTrackEntity;

import java.util.List;

@Dao
public interface ScoutingGpsTrackDao {

    // ============================================================
    // INSERT
    // ============================================================

    @Insert
    long insert(
            ScoutingGpsTrackEntity track
    );

    @Insert
    void insertAll(
            List<ScoutingGpsTrackEntity> tracks
    );


    // ============================================================
    // GENERAL QUERIES
    // ============================================================

    @Query(
            "SELECT * FROM scouting_gps_tracks " +
                    "ORDER BY recorded_at ASC"
    )
    List<ScoutingGpsTrackEntity> getAll();

    @Query(
            "SELECT * FROM scouting_gps_tracks " +
                    "WHERE id = :id " +
                    "LIMIT 1"
    )
    ScoutingGpsTrackEntity getById(
            long id
    );

    @Query(
            "SELECT * FROM scouting_gps_tracks " +
                    "WHERE client_uuid = :clientUuid " +
                    "LIMIT 1"
    )
    ScoutingGpsTrackEntity getByClientUuid(
            String clientUuid
    );


    // ============================================================
    // ROUND
    // ============================================================

    @Query(
            "SELECT * FROM scouting_gps_tracks " +
                    "WHERE round_id = :roundId " +
                    "ORDER BY recorded_at ASC"
    )
    List<ScoutingGpsTrackEntity> getByRound(
            long roundId
    );


    // ============================================================
    // SYNC
    // ============================================================

    @Query(
            "SELECT * FROM scouting_gps_tracks " +
                    "WHERE sync_status IS NULL " +
                    "OR sync_status = 'pending' " +
                    "OR sync_status = 'failed' " +
                    "ORDER BY recorded_at ASC"
    )
    List<ScoutingGpsTrackEntity> getPendingSync();

    @Query(
            "SELECT * FROM scouting_gps_tracks " +
                    "WHERE sync_status = 'failed' " +
                    "ORDER BY recorded_at ASC"
    )
    List<ScoutingGpsTrackEntity> getFailedSync();


    // ============================================================
    // SYNC STATUS
    // ============================================================

    @Query(
            "UPDATE scouting_gps_tracks " +
                    "SET sync_status = 'syncing', sync_error = NULL " +
                    "WHERE id = :id"
    )
    void markSyncing(
            long id
    );

    @Query(
            "UPDATE scouting_gps_tracks " +
                    "SET sync_status = 'synced', sync_error = NULL " +
                    "WHERE id = :id"
    )
    void markSynced(
            long id
    );

    @Query(
            "UPDATE scouting_gps_tracks " +
                    "SET sync_status = 'failed', sync_error = :error " +
                    "WHERE id = :id"
    )
    void markFailed(
            long id,
            String error
    );

    @Query(
            "UPDATE scouting_gps_tracks " +
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
            "SELECT COUNT(*) FROM scouting_gps_tracks"
    )
    int count();

    @Query(
            "SELECT COUNT(*) FROM scouting_gps_tracks " +
                    "WHERE round_id = :roundId"
    )
    int countByRound(
            long roundId
    );

    @Query(
            "SELECT COUNT(*) FROM scouting_gps_tracks " +
                    "WHERE sync_status IS NULL " +
                    "OR sync_status = 'pending' " +
                    "OR sync_status = 'failed'"
    )
    int countPendingSync();

    @Query(
            "SELECT COUNT(*) FROM scouting_gps_tracks " +
                    "WHERE sync_status = 'failed'"
    )
    int countFailedSync();


    // ============================================================
    // DELETE
    // ============================================================

    @Query(
            "DELETE FROM scouting_gps_tracks"
    )
    void deleteAll();

    @Query(
            "DELETE FROM scouting_gps_tracks " +
                    "WHERE round_id = :roundId"
    )
    void deleteByRound(
            long roundId
    );

    @Query(
            "UPDATE scouting_gps_tracks " +
                    "SET sync_status = 'pending', sync_error = NULL " +
                    "WHERE sync_status = 'syncing'"
    )
    void resetSyncingToPending();
}