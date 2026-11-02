package com.aaagrowers.scoutingapp.data.sync;

import com.aaagrowers.scoutingapp.data.database.dao.TrapObservationDao;
import com.aaagrowers.scoutingapp.data.database.entity.TrapObservationEntity;
import com.aaagrowers.scoutingapp.network.ApiClient;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

public class TrapObservationSyncRepository {

    private final TrapObservationDao trapObservationDao;

    private final ApiClient apiClient;


    /*
     * ================================================================
     * CONSTRUCTOR
     * ================================================================
     */

    public TrapObservationSyncRepository(
            TrapObservationDao trapObservationDao
    ) {

        this.trapObservationDao =
                trapObservationDao;

        this.apiClient =
                new ApiClient();
    }


    /*
     * ================================================================
     * SYNC ALL PENDING TRAP OBSERVATIONS
     * ================================================================
     */

    public void syncAll() throws Exception {

        List<TrapObservationEntity> observations =
                trapObservationDao.getPendingObservations();


        if (
                observations == null
                        ||
                        observations.isEmpty()
        ) {

            return;
        }


        /*
         * ------------------------------------------------------------
         * SEND EACH OBSERVATION
         * ------------------------------------------------------------
         */

        for (
                TrapObservationEntity observation
                : observations
        ) {

            syncObservation(
                    observation
            );
        }
    }


    /*
     * ================================================================
     * SYNC ONE OBSERVATION
     * ================================================================
     */

    private void syncObservation(
            TrapObservationEntity observation
    ) throws Exception {

        JSONObject item =
                new JSONObject();


        /*
         * ------------------------------------------------------------
         * LOCAL ID
         * ------------------------------------------------------------
         */

        item.put(
                "local_id",
                observation.id
        );


        /*
         * ------------------------------------------------------------
         * CLIENT UUID
         * ------------------------------------------------------------
         */

        item.put(
                "client_uuid",
                observation.client_uuid
        );


        /*
         * ------------------------------------------------------------
         * TRAP
         * ------------------------------------------------------------
         */

        item.put(
                "trap_id",
                observation.trap_id
        );

        item.put(
                "trap_code",
                observation.trap_code
        );

        item.put(
                "trap_type",
                observation.trap_type
        );


        /*
         * ------------------------------------------------------------
         * INSECT TYPE
         * ------------------------------------------------------------
         */

        if (
                observation.insect_type_id != null
        ) {

            item.put(
                    "insect_type_id",
                    observation.insect_type_id
            );

        } else {

            item.put(
                    "insect_type_id",
                    JSONObject.NULL
            );
        }


        /*
         * ------------------------------------------------------------
         * COUNT
         * ------------------------------------------------------------
         */

        item.put(
                "moth_count",
                observation.moth_count
        );


        /*
         * ------------------------------------------------------------
         * TREND
         * ------------------------------------------------------------
         *
         * Android:
         *
         * decreasing
         * stable
         * increasing
         *
         * Server:
         *
         * worsening
         * stable
         * improving
         *
         * ------------------------------------------------------------
         */

        String serverTrend =
                null;


        if (
                "decreasing".equalsIgnoreCase(
                        observation.trend
                )
        ) {

            serverTrend =
                    "improving";

        } else if (
                "increasing".equalsIgnoreCase(
                        observation.trend
                )
        ) {

            serverTrend =
                    "worsening";

        } else if (
                "stable".equalsIgnoreCase(
                        observation.trend
                )
        ) {

            serverTrend =
                    "stable";
        }


        if (serverTrend != null) {

            item.put(
                    "trend",
                    serverTrend
            );

        } else {

            item.put(
                    "trend",
                    JSONObject.NULL
            );
        }


        /*
         * ------------------------------------------------------------
         * COMMENTS
         * ------------------------------------------------------------
         */

        if (
                observation.comments != null
        ) {

            item.put(
                    "comments",
                    observation.comments
            );

        } else {

            item.put(
                    "comments",
                    JSONObject.NULL
            );
        }


        /*
         * ------------------------------------------------------------
         * GPS
         * ------------------------------------------------------------
         */

        if (
                observation.latitude != null
        ) {

            item.put(
                    "latitude",
                    observation.latitude
            );

        } else {

            item.put(
                    "latitude",
                    JSONObject.NULL
            );
        }


        if (
                observation.longitude != null
        ) {

            item.put(
                    "longitude",
                    observation.longitude
            );

        } else {

            item.put(
                    "longitude",
                    JSONObject.NULL
            );
        }


        if (
                observation.gps_accuracy != null
        ) {

            item.put(
                    "gps_accuracy",
                    observation.gps_accuracy
            );

        } else {

            item.put(
                    "gps_accuracy",
                    JSONObject.NULL
            );
        }


        /*
         * ------------------------------------------------------------
         * OBSERVED AT
         * ------------------------------------------------------------
         */

        item.put(
                "observed_at",
                observation.observed_at
        );


        /*
         * ------------------------------------------------------------
         * USER
         * ------------------------------------------------------------
         */

        item.put(
                "user_id",
                observation.user_id
        );


        /*
         * ------------------------------------------------------------
         * PHOTO FILE
         * ------------------------------------------------------------
         */

        File photoFile =
                null;


        if (
                observation.photo_path != null
                        &&
                        !observation.photo_path
                                .trim()
                                .isEmpty()
        ) {

            photoFile =
                    new File(
                            observation.photo_path
                    );


            if (
                    !photoFile.exists()
                            ||
                            !photoFile.isFile()
                            ||
                            photoFile.length() <= 0
            ) {

                throw new Exception(
                        "Photo file could not be found for observation "
                                + observation.client_uuid
                );
            }
        }


        /*
         * ------------------------------------------------------------
         * SEND
         * ------------------------------------------------------------
         */

        JSONObject response =
                apiClient.syncTrapObservations(
                        item,
                        photoFile
                );


        /*
         * ------------------------------------------------------------
         * CHECK RESPONSE
         * ------------------------------------------------------------
         */

        boolean success =
                response.optBoolean(
                        "success",
                        false
                );


        if (!success) {

            String message =
                    response.optString(
                            "message",
                            "Trap observation synchronization failed."
                    );


            trapObservationDao.updateSyncStatus(
                    observation.id,
                    "failed",
                    message
            );


            throw new Exception(
                    message
            );
        }


        /*
         * ------------------------------------------------------------
         * SERVER PHOTO PATH
         * ------------------------------------------------------------
         */

        JSONObject responseData =
                response.optJSONObject(
                        "data"
                );


        if (
                responseData != null
        ) {

            String serverPhotoPath =
                    responseData.optString(
                            "photo_path",
                            ""
                    );


            /*
             * We intentionally do not overwrite the local photo_path
             * here.
             *
             * The Android application still needs the local path
             * if we want to retain the photograph locally.
             */
        }


        /*
         * ------------------------------------------------------------
         * MARK SYNCED
         * ------------------------------------------------------------
         */

        trapObservationDao.updateSyncStatus(
                observation.id,
                "synced",
                null
        );
    }
}