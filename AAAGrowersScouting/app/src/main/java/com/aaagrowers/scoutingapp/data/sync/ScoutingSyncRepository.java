package com.aaagrowers.scoutingapp.data.sync;

import com.aaagrowers.scoutingapp.data.database.dao.ScoutingGpsTrackDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScoutingObservationDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScoutingRoundDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScoutingSeverePointDao;
import com.aaagrowers.scoutingapp.data.database.entity.ScoutingGpsTrackEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScoutingObservationEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScoutingRoundEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScoutingSeverePointEntity;

import java.util.List;

import com.aaagrowers.scoutingapp.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

public class ScoutingSyncRepository {

    private final ScoutingRoundDao roundDao;
    private final ScoutingObservationDao observationDao;
    private final ScoutingSeverePointDao severePointDao;
    private final ScoutingGpsTrackDao gpsTrackDao;

    private final ApiClient apiClient;


    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public ScoutingSyncRepository(
            ScoutingRoundDao roundDao,
            ScoutingObservationDao observationDao,
            ScoutingSeverePointDao severePointDao,
            ScoutingGpsTrackDao gpsTrackDao
    ) {

        this.roundDao = roundDao;
        this.observationDao = observationDao;
        this.severePointDao = severePointDao;
        this.gpsTrackDao = gpsTrackDao;

        this.apiClient = new ApiClient();
    }


    // ============================================================
    // PENDING COUNTS
    // ============================================================

    public int getPendingRoundCount() {

        return roundDao.countPendingSync();
    }


    public int getPendingObservationCount() {

        return observationDao.countPendingSync();
    }


    public int getPendingSeverePointCount() {

        return severePointDao.countPendingSync();
    }


    public int getPendingGpsTrackCount() {

        return gpsTrackDao.countPendingSync();
    }


    public int getTotalPendingCount() {

        return
                getPendingRoundCount()
                        + getPendingObservationCount()
                        + getPendingSeverePointCount()
                        + getPendingGpsTrackCount();
    }


    // ============================================================
    // FAILED COUNTS
    // ============================================================

    public int getFailedRoundCount() {

        return roundDao.countFailedSync();
    }


    public int getFailedObservationCount() {

        return observationDao.countFailedSync();
    }


    public int getFailedSeverePointCount() {

        return severePointDao.countFailedSync();
    }


    public int getFailedGpsTrackCount() {

        return gpsTrackDao.countFailedSync();
    }


    public int getTotalFailedCount() {

        return
                getFailedRoundCount()
                        + getFailedObservationCount()
                        + getFailedSeverePointCount()
                        + getFailedGpsTrackCount();
    }


    // ============================================================
    // GET PENDING DATA
    // ============================================================

    public List<ScoutingRoundEntity> getPendingRounds() {

        return roundDao.getPendingSync();
    }


    public List<ScoutingObservationEntity> getPendingObservations() {

        return observationDao.getPendingSync();
    }


    public List<ScoutingSeverePointEntity> getPendingSeverePoints() {

        return severePointDao.getPendingSync();
    }


    public List<ScoutingGpsTrackEntity> getPendingGpsTracks() {

        return gpsTrackDao.getPendingSync();
    }


    // ============================================================
    // MARK ROUND
    // ============================================================

    public void markRoundSyncing(
            long id
    ) {

        roundDao.markSyncing(id);
    }


    public void markRoundSynced(
            long id
    ) {

        roundDao.markSynced(id);
    }


    public void markRoundFailed(
            long id,
            String error
    ) {

        roundDao.markFailed(
                id,
                error
        );
    }


    public void markRoundPending(
            long id
    ) {

        roundDao.markPending(id);
    }


    // ============================================================
    // MARK OBSERVATION
    // ============================================================

    public void markObservationSyncing(
            long id
    ) {

        observationDao.markSyncing(id);
    }


    public void markObservationSynced(
            long id
    ) {

        observationDao.markSynced(id);
    }


    public void markObservationFailed(
            long id,
            String error
    ) {

        observationDao.markFailed(
                id,
                error
        );
    }


    public void markObservationPending(
            long id
    ) {

        observationDao.markPending(id);
    }


    // ============================================================
    // MARK SEVERE POINT
    // ============================================================

    public void markSeverePointSyncing(
            long id
    ) {

        severePointDao.markSyncing(id);
    }


    public void markSeverePointSynced(
            long id
    ) {

        severePointDao.markSynced(id);
    }


    public void markSeverePointFailed(
            long id,
            String error
    ) {

        severePointDao.markFailed(
                id,
                error
        );
    }


    public void markSeverePointPending(
            long id
    ) {

        severePointDao.markPending(id);
    }


    // ============================================================
    // MARK GPS TRACK
    // ============================================================

    public void markGpsTrackSyncing(
            long id
    ) {

        gpsTrackDao.markSyncing(id);
    }


    public void markGpsTrackSynced(
            long id
    ) {

        gpsTrackDao.markSynced(id);
    }


    public void markGpsTrackFailed(
            long id,
            String error
    ) {

        gpsTrackDao.markFailed(
                id,
                error
        );
    }


    public void markGpsTrackPending(
            long id
    ) {

        gpsTrackDao.markPending(id);
    }


    // ============================================================
    // ROUND WITH CHILD DATA
    // ============================================================

    public List<ScoutingObservationEntity> getObservationsForRound(
            long roundId
    ) {

        return observationDao.getByRound(roundId);
    }


    public List<ScoutingSeverePointEntity> getSeverePointsForRound(
            long roundId
    ) {

        return severePointDao.getByRound(roundId);
    }


    public List<ScoutingGpsTrackEntity> getGpsTracksForRound(
            long roundId
    ) {

        return gpsTrackDao.getByRound(roundId);
    }


    // ============================================================
    // RETRY FAILED ROUND
    // ============================================================

    public void retryRound(
            long id
    ) {

        roundDao.markPending(id);
    }


    // ============================================================
    // RETRY FAILED OBSERVATION
    // ============================================================

    public void retryObservation(
            long id
    ) {

        observationDao.markPending(id);
    }


    // ============================================================
    // RETRY FAILED SEVERE POINT
    // ============================================================

    public void retrySeverePoint(
            long id
    ) {

        severePointDao.markPending(id);
    }


    // ============================================================
    // RETRY FAILED GPS TRACK
    // ============================================================

    public void retryGpsTrack(
            long id
    ) {

        gpsTrackDao.markPending(id);
    }


    // ============================================================
    // RETRY EVERYTHING FAILED
    // ============================================================

    public void retryAllFailed() {

        List<ScoutingRoundEntity> rounds =
                roundDao.getFailedSync();

        for (ScoutingRoundEntity round : rounds) {

            android.util.Log.d(
                    "SCOUTING_SYNC",
                    "SYNCING ROUND ID = " + round.id +
                            " UUID = " + round.client_uuid
            );

            roundDao.markPending(
                    round.id
            );
        }


        List<ScoutingObservationEntity> observations =
                observationDao.getFailedSync();

        for (ScoutingObservationEntity observation : observations) {

            observationDao.markPending(
                    observation.id
            );
        }


        List<ScoutingSeverePointEntity> severePoints =
                severePointDao.getFailedSync();

        for (ScoutingSeverePointEntity severePoint : severePoints) {

            severePointDao.markPending(
                    severePoint.id
            );
        }


        List<ScoutingGpsTrackEntity> gpsTracks =
                gpsTrackDao.getFailedSync();

        for (ScoutingGpsTrackEntity track : gpsTracks) {

            gpsTrackDao.markPending(
                    track.id
            );
        }
    }

// ============================================================
// SYNC ALL SCOUTING DATA
// ============================================================

    public void syncAll() throws Exception {

        /*
         * ============================================================
         * RESET RECORDS THAT MAY HAVE BEEN LEFT IN SYNCING STATE
         * ============================================================
         */

        resetSyncingRecords();


        /*
         * ============================================================
         * GET PENDING ROUNDS
         * ============================================================
         */

        List<ScoutingRoundEntity> rounds =
                roundDao.getPendingSync();


        android.util.Log.d(
                "SCOUTING_SYNC",
                "Pending rounds: " + rounds.size()
        );

        android.util.Log.d(
                "SCOUTING_SYNC",
                "Pending observations: " +
                        observationDao.countPendingSync()
        );

        android.util.Log.d(
                "SCOUTING_SYNC",
                "Pending severe points: " +
                        severePointDao.countPendingSync()
        );

        android.util.Log.d(
                "SCOUTING_SYNC",
                "Pending GPS tracks: " +
                        gpsTrackDao.countPendingSync()
        );


        /*
         * ============================================================
         * SYNC EACH ROUND
         * ============================================================
         */

        for (ScoutingRoundEntity round : rounds) {

            try {

                android.util.Log.d(
                        "SCOUTING_SYNC",
                        "Starting round sync: ID="
                                + round.id
                                + " UUID="
                                + round.client_uuid
                );


                /*
                 * ====================================================
                 * MARK ROUND AS SYNCING
                 * ====================================================
                 */

                markRoundSyncing(
                        round.id
                );


                /*
                 * ====================================================
                 * BUILD ROUND JSON
                 * ====================================================
                 */

                JSONObject roundJson =
                        new JSONObject();


                roundJson.put(
                        "client_uuid",
                        round.client_uuid
                );


                roundJson.put(
                        "scout_id",
                        round.scout_id
                );


                /*
                 * ----------------------------------------------------
                 * section_id IS INT
                 *
                 * 0 = no section
                 * ----------------------------------------------------
                 */

                if (round.section_id > 0) {

                    roundJson.put(
                            "section_id",
                            round.section_id
                    );

                } else {

                    roundJson.put(
                            "section_id",
                            JSONObject.NULL
                    );
                }


                /*
                 * ----------------------------------------------------
                 * round_date
                 * ----------------------------------------------------
                 */

                if (round.round_date != null) {

                    roundJson.put(
                            "round_date",
                            round.round_date
                    );

                } else {

                    roundJson.put(
                            "round_date",
                            JSONObject.NULL
                    );
                }


                /*
                 * ----------------------------------------------------
                 * started_at
                 * ----------------------------------------------------
                 */

                if (round.started_at != null) {

                    roundJson.put(
                            "started_at",
                            round.started_at
                    );

                } else {

                    roundJson.put(
                            "started_at",
                            JSONObject.NULL
                    );
                }


                /*
                 * ----------------------------------------------------
                 * completed_at
                 * ----------------------------------------------------
                 */

                if (round.completed_at != null) {

                    roundJson.put(
                            "completed_at",
                            round.completed_at
                    );

                } else {

                    roundJson.put(
                            "completed_at",
                            JSONObject.NULL
                    );
                }


                /*
                 * ----------------------------------------------------
                 * status
                 * ----------------------------------------------------
                 */

                if (round.status != null) {

                    roundJson.put(
                            "status",
                            round.status
                    );

                } else {

                    roundJson.put(
                            "status",
                            "open"
                    );
                }


                /*
                 * ----------------------------------------------------
                 * notes
                 * ----------------------------------------------------
                 */

                if (round.notes != null) {

                    roundJson.put(
                            "notes",
                            round.notes
                    );

                } else {

                    roundJson.put(
                            "notes",
                            JSONObject.NULL
                    );
                }


                /*
                 * ----------------------------------------------------
                 * created_at
                 * ----------------------------------------------------
                 */

                if (round.created_at != null) {

                    roundJson.put(
                            "created_at",
                            round.created_at
                    );

                } else {

                    roundJson.put(
                            "created_at",
                            JSONObject.NULL
                    );
                }


                /*
                 * ====================================================
                 * GET CHILD RECORDS
                 * ====================================================
                 */

                List<ScoutingObservationEntity> observations =
                        observationDao.getByRound(
                                round.id
                        );


                List<ScoutingSeverePointEntity> severePoints =
                        severePointDao.getByRound(
                                round.id
                        );


                List<ScoutingGpsTrackEntity> gpsTracks =
                        gpsTrackDao.getByRound(
                                round.id
                        );


                android.util.Log.d(
                        "SCOUTING_SYNC",
                        "Round "
                                + round.id
                                + " children: observations="
                                + observations.size()
                                + ", severe="
                                + severePoints.size()
                                + ", gps="
                                + gpsTracks.size()
                );


                /*
                 * ====================================================
                 * BUILD OBSERVATIONS ARRAY
                 * ====================================================
                 */

                JSONArray observationArray =
                        new JSONArray();


                /*
                 * Keep track of records actually sent.
                 */

                List<ScoutingObservationEntity> observationsToSync =
                        new java.util.ArrayList<>();


                for (ScoutingObservationEntity observation
                        : observations) {


                    /*
                     * ------------------------------------------------
                     * Skip records already synchronized.
                     * ------------------------------------------------
                     */

                    if (
                            "synced".equalsIgnoreCase(
                                    observation.sync_status
                            )
                    ) {

                        continue;
                    }


                    JSONObject json =
                            new JSONObject();


                    json.put(
                            "client_uuid",
                            observation.client_uuid
                    );


                    /*
                     * ------------------------------------------------
                     * scanning_point_id IS INT
                     *
                     * 0 = no scanning point
                     * ------------------------------------------------
                     */

                    if (observation.scanning_point_id > 0) {

                        json.put(
                                "scanning_point_id",
                                observation.scanning_point_id
                        );

                    } else {

                        json.put(
                                "scanning_point_id",
                                JSONObject.NULL
                        );
                    }


                    /*
                     * ------------------------------------------------
                     * greenhouse_variety_id IS INT
                     *
                     * 0 = no variety
                     * ------------------------------------------------
                     */

                    if (observation.greenhouse_variety_id > 0) {

                        json.put(
                                "greenhouse_variety_id",
                                observation.greenhouse_variety_id
                        );

                    } else {

                        json.put(
                                "greenhouse_variety_id",
                                JSONObject.NULL
                        );
                    }


                    /*
                     * ------------------------------------------------
                     * observation_type_id
                     * ------------------------------------------------
                     */

                    json.put(
                            "observation_type_id",
                            observation.observation_type_id
                    );


                    /*
                     * ------------------------------------------------
                     * score
                     * ------------------------------------------------
                     */

                    json.put(
                            "score",
                            observation.score
                    );


                    /*
                     * ------------------------------------------------
                     * trend
                     * ------------------------------------------------
                     */

                    if (observation.trend != null) {

                        json.put(
                                "trend",
                                observation.trend
                        );

                    } else {

                        json.put(
                                "trend",
                                JSONObject.NULL
                        );
                    }


                    /*
                     * ------------------------------------------------
                     * comment
                     * ------------------------------------------------
                     */

                    if (observation.comment != null) {

                        json.put(
                                "comment",
                                observation.comment
                        );

                    } else {

                        json.put(
                                "comment",
                                JSONObject.NULL
                        );
                    }


                    /*
                     * ------------------------------------------------
                     * photo_path
                     * ------------------------------------------------
                     */

                    if (observation.photo_path != null) {

                        json.put(
                                "photo_path",
                                observation.photo_path
                        );

                    } else {

                        json.put(
                                "photo_path",
                                JSONObject.NULL
                        );
                    }


                    /*
                     * ------------------------------------------------
                     * observed_at
                     * ------------------------------------------------
                     */

                    if (observation.observed_at != null) {

                        json.put(
                                "observed_at",
                                observation.observed_at
                        );

                    } else {

                        json.put(
                                "observed_at",
                                JSONObject.NULL
                        );
                    }


                    /*
                     * ------------------------------------------------
                     * created_at
                     * ------------------------------------------------
                     */

                    if (observation.created_at != null) {

                        json.put(
                                "created_at",
                                observation.created_at
                        );

                    } else {

                        json.put(
                                "created_at",
                                JSONObject.NULL
                        );
                    }


                    observationArray.put(
                            json
                    );


                    /*
                     * Remember this record was actually included.
                     */

                    observationsToSync.add(
                            observation
                    );
                }


                /*
                 * ====================================================
                 * BUILD SEVERE POINTS ARRAY
                 * ====================================================
                 */

                JSONArray severeArray =
                        new JSONArray();


                List<ScoutingSeverePointEntity> severePointsToSync =
                        new java.util.ArrayList<>();


                for (ScoutingSeverePointEntity point
                        : severePoints) {


                    /*
                     * ------------------------------------------------
                     * Skip already synchronized records.
                     * ------------------------------------------------
                     */

                    if (
                            "synced".equalsIgnoreCase(
                                    point.sync_status
                            )
                    ) {

                        continue;
                    }


                    JSONObject json =
                            new JSONObject();


                    json.put(
                            "client_uuid",
                            point.client_uuid
                    );


                    /*
                     * ------------------------------------------------
                     * scanning_point_id IS INT
                     * ------------------------------------------------
                     */

                    if (point.scanning_point_id > 0) {

                        json.put(
                                "scanning_point_id",
                                point.scanning_point_id
                        );

                    } else {

                        json.put(
                                "scanning_point_id",
                                JSONObject.NULL
                        );
                    }


                    /*
                     * ------------------------------------------------
                     * bed_id
                     * ------------------------------------------------
                     */

                    json.put(
                            "bed_id",
                            point.bed_id
                    );


                    /*
                     * ------------------------------------------------
                     * observation_type_id
                     * ------------------------------------------------
                     */

                    json.put(
                            "observation_type_id",
                            point.observation_type_id
                    );


                    /*
                     * ------------------------------------------------
                     * score
                     * ------------------------------------------------
                     */

                    json.put(
                            "score",
                            point.score
                    );


                    /*
                     * ------------------------------------------------
                     * trend
                     * ------------------------------------------------
                     */

                    if (point.trend != null) {

                        json.put(
                                "trend",
                                point.trend
                        );

                    } else {

                        json.put(
                                "trend",
                                JSONObject.NULL
                        );
                    }


                    /*
                     * ------------------------------------------------
                     * latitude
                     * ------------------------------------------------
                     */

                    json.put(
                            "latitude",
                            point.latitude
                    );


                    /*
                     * ------------------------------------------------
                     * longitude
                     * ------------------------------------------------
                     */

                    json.put(
                            "longitude",
                            point.longitude
                    );


                    /*
                     * ------------------------------------------------
                     * gps_accuracy IS DOUBLE
                     *
                     * 0 = unavailable
                     * ------------------------------------------------
                     */

                    if (point.gps_accuracy > 0) {

                        json.put(
                                "gps_accuracy",
                                point.gps_accuracy
                        );

                    } else {

                        json.put(
                                "gps_accuracy",
                                JSONObject.NULL
                        );
                    }


                    /*
                     * ------------------------------------------------
                     * comment
                     * ------------------------------------------------
                     */

                    if (point.comment != null) {

                        json.put(
                                "comment",
                                point.comment
                        );

                    } else {

                        json.put(
                                "comment",
                                JSONObject.NULL
                        );
                    }


                    /*
                     * ------------------------------------------------
                     * photo_path
                     * ------------------------------------------------
                     */

                    if (point.photo_path != null) {

                        json.put(
                                "photo_path",
                                point.photo_path
                        );

                    } else {

                        json.put(
                                "photo_path",
                                JSONObject.NULL
                        );
                    }


                    /*
                     * ------------------------------------------------
                     * observed_at
                     * ------------------------------------------------
                     */

                    if (point.observed_at != null) {

                        json.put(
                                "observed_at",
                                point.observed_at
                        );

                    } else {

                        json.put(
                                "observed_at",
                                JSONObject.NULL
                        );
                    }


                    /*
                     * ------------------------------------------------
                     * created_at
                     * ------------------------------------------------
                     */

                    if (point.created_at != null) {

                        json.put(
                                "created_at",
                                point.created_at
                        );

                    } else {

                        json.put(
                                "created_at",
                                JSONObject.NULL
                        );
                    }


                    severeArray.put(
                            json
                    );


                    /*
                     * Remember this record was actually included.
                     */

                    severePointsToSync.add(
                            point
                    );
                }


                /*
                 * ====================================================
                 * BUILD GPS TRACKS ARRAY
                 * ====================================================
                 */

                JSONArray gpsArray =
                        new JSONArray();


                List<ScoutingGpsTrackEntity> gpsTracksToSync =
                        new java.util.ArrayList<>();


                for (ScoutingGpsTrackEntity track
                        : gpsTracks) {


                    /*
                     * ------------------------------------------------
                     * Skip already synchronized records.
                     * ------------------------------------------------
                     */

                    if (
                            "synced".equalsIgnoreCase(
                                    track.sync_status
                            )
                    ) {

                        continue;
                    }


                    JSONObject json =
                            new JSONObject();


                    json.put(
                            "client_uuid",
                            track.client_uuid
                    );


                    /*
                     * ------------------------------------------------
                     * latitude
                     * ------------------------------------------------
                     */

                    json.put(
                            "latitude",
                            track.latitude
                    );


                    /*
                     * ------------------------------------------------
                     * longitude
                     * ------------------------------------------------
                     */

                    json.put(
                            "longitude",
                            track.longitude
                    );


                    /*
                     * ------------------------------------------------
                     * accuracy IS DOUBLE
                     *
                     * 0 = unavailable
                     * ------------------------------------------------
                     */

                    if (track.accuracy > 0) {

                        json.put(
                                "accuracy",
                                track.accuracy
                        );

                    } else {

                        json.put(
                                "accuracy",
                                JSONObject.NULL
                        );
                    }


                    /*
                     * ------------------------------------------------
                     * altitude IS DOUBLE
                     *
                     * Preserve 0 as a valid GPS value.
                     * ------------------------------------------------
                     */

                    json.put(
                            "altitude",
                            track.altitude
                    );


                    /*
                     * ------------------------------------------------
                     * speed IS DOUBLE
                     *
                     * 0 is a valid stationary speed.
                     * ------------------------------------------------
                     */

                    json.put(
                            "speed",
                            track.speed
                    );


                    /*
                     * ------------------------------------------------
                     * bearing IS DOUBLE
                     *
                     * 0 is a valid bearing.
                     * ------------------------------------------------
                     */

                    json.put(
                            "bearing",
                            track.bearing
                    );


                    /*
                     * ------------------------------------------------
                     * recorded_at
                     * ------------------------------------------------
                     */

                    if (track.recorded_at != null) {

                        json.put(
                                "recorded_at",
                                track.recorded_at
                        );

                    } else {

                        json.put(
                                "recorded_at",
                                JSONObject.NULL
                        );
                    }


                    /*
                     * ------------------------------------------------
                     * created_at
                     * ------------------------------------------------
                     */

                    if (track.created_at != null) {

                        json.put(
                                "created_at",
                                track.created_at
                        );

                    } else {

                        json.put(
                                "created_at",
                                JSONObject.NULL
                        );
                    }


                    gpsArray.put(
                            json
                    );


                    /*
                     * Remember this record was actually included.
                     */

                    gpsTracksToSync.add(
                            track
                    );
                }


                /*
                 * ====================================================
                 * COMPLETE PAYLOAD
                 * ====================================================
                 */

                JSONObject payload =
                        new JSONObject();


                payload.put(
                        "round",
                        roundJson
                );


                payload.put(
                        "observations",
                        observationArray
                );


                payload.put(
                        "severe_points",
                        severeArray
                );


                payload.put(
                        "gps_tracks",
                        gpsArray
                );


                /*
                 * ====================================================
                 * LOG PAYLOAD
                 * ====================================================
                 */

                android.util.Log.d(
                        "SCOUTING_SYNC",
                        "Sending payload for round "
                                + round.client_uuid
                );


                android.util.Log.d(
                        "SCOUTING_SYNC",
                        payload.toString()
                );


                /*
                 * ====================================================
                 * SEND TO PHP API
                 * ====================================================
                 */

                String response =
                        apiClient.syncScoutingData(
                                payload
                        );


                /*
                 * ====================================================
                 * LOG SERVER RESPONSE
                 * ====================================================
                 */

                android.util.Log.d(
                        "SCOUTING_SYNC",
                        "Server response: "
                                + response
                );


                /*
                 * ====================================================
                 * PARSE SERVER RESPONSE
                 * ====================================================
                 */

                JSONObject responseJson =
                        new JSONObject(
                                response
                        );


                boolean success =
                        responseJson.optBoolean(
                                "success",
                                false
                        );


                if (!success) {

                    throw new Exception(
                            responseJson.optString(
                                    "message",
                                    "Server synchronization failed."
                            )
                    );
                }


                /*
                 * ====================================================
                 * MARK OBSERVATIONS THAT WERE ACTUALLY SENT
                 * ====================================================
                 */

                for (
                        ScoutingObservationEntity observation
                        : observationsToSync
                ) {

                    markObservationSynced(
                            observation.id
                    );
                }


                /*
                 * ====================================================
                 * MARK SEVERE POINTS THAT WERE ACTUALLY SENT
                 * ====================================================
                 */

                for (
                        ScoutingSeverePointEntity point
                        : severePointsToSync
                ) {

                    markSeverePointSynced(
                            point.id
                    );
                }


                /*
                 * ====================================================
                 * MARK GPS TRACKS THAT WERE ACTUALLY SENT
                 * ====================================================
                 */

                for (
                        ScoutingGpsTrackEntity track
                        : gpsTracksToSync
                ) {

                    markGpsTrackSynced(
                            track.id
                    );
                }


                /*
                 * ====================================================
                 * MARK ROUND SYNCED LAST
                 * ====================================================
                 */

                markRoundSynced(
                        round.id
                );


                android.util.Log.d(
                        "SCOUTING_SYNC",
                        "Round synchronized successfully: "
                                + round.client_uuid
                );

            } catch (Exception e) {

                /*
                 * ====================================================
                 * ROUND FAILED
                 * ====================================================
                 */

                String error =
                        e.getMessage();


                if (error == null
                        ||
                        error.trim().isEmpty()) {

                    error =
                            e.getClass()
                                    .getSimpleName();
                }


                android.util.Log.e(
                        "SCOUTING_SYNC",
                        "Round synchronization failed: "
                                + round.client_uuid
                                + " - "
                                + error,
                        e
                );


                /*
                 * ----------------------------------------------------
                 * Mark only the round as failed here.
                 *
                 * Child records remain locally available and can
                 * be retried when the round is retried.
                 * ----------------------------------------------------
                 */

                markRoundFailed(
                        round.id,
                        error
                );


                /*
                 * ====================================================
                 * DO NOT DELETE LOCAL DATA
                 * ====================================================
                 */

                throw e;
            }
        }


        /*
         * ============================================================
         * LOG COMPLETION
         * ============================================================
         */

        android.util.Log.d(
                "SCOUTING_SYNC",
                "Scouting synchronization completed."
        );
    }

    public void resetSyncingRecords() {

        roundDao.resetSyncingToPending();

        observationDao.resetSyncingToPending();

        severePointDao.resetSyncingToPending();

        gpsTrackDao.resetSyncingToPending();
    }
}