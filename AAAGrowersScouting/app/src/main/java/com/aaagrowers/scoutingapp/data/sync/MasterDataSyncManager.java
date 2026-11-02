package com.aaagrowers.scoutingapp.data.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.aaagrowers.scoutingapp.data.model.MasterDataPayload;
import com.aaagrowers.scoutingapp.data.model.MasterDataResponse;
import com.aaagrowers.scoutingapp.data.model.MasterDataSyncResult;
import com.aaagrowers.scoutingapp.data.repository.MasterDataRepository;
import com.aaagrowers.scoutingapp.network.ApiClient;
import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MasterDataSyncManager {

    /*
     * ================================================================
     * LOGGING
     * ================================================================
     */

    private static final String TAG =
            "MASTER_SYNC";


    /*
     * ================================================================
     * CONTEXT
     * ================================================================
     */

    private final Context context;


    /*
     * ================================================================
     * REPOSITORY
     * ================================================================
     */

    private final MasterDataRepository repository;


    /*
     * ================================================================
     * THREADING
     * ================================================================
     */

    private final ExecutorService executorService;

    private final Handler mainHandler;


    /*
     * ================================================================
     * JSON
     * ================================================================
     */

    private final Gson gson;


    /*
     * ================================================================
     * CONSTRUCTOR
     * ================================================================
     */

    public MasterDataSyncManager(
            @NonNull Context context
    ) {

        this.context =
                context.getApplicationContext();


        repository =
                new MasterDataRepository(
                        this.context
                );


        executorService =
                Executors.newSingleThreadExecutor();


        mainHandler =
                new Handler(
                        Looper.getMainLooper()
                );


        gson =
                new Gson();
    }


    /*
     * ================================================================
     * SYNCHRONIZE MASTER DATA
     * ================================================================
     */

    public void sync(
            SyncCallback callback
    ) {

        executorService.execute(
                () -> {

                    try {

                        Log.d(
                                TAG,
                                "========================================"
                        );

                        Log.d(
                                TAG,
                                "MASTER DATA SYNC STARTED"
                        );

                        Log.d(
                                TAG,
                                "========================================"
                        );


                        /*
                         * ------------------------------------------------
                         * API CLIENT
                         * ------------------------------------------------
                         */

                        ApiClient apiClient =
                                new ApiClient();


                        /*
                         * ------------------------------------------------
                         * REQUEST RAW JSON
                         * ------------------------------------------------
                         */

                        Log.d(
                                TAG,
                                "Requesting master data from server..."
                        );


                        String json =
                                apiClient.getMasterData();


                        /*
                         * ------------------------------------------------
                         * LOG RAW RESPONSE
                         * ------------------------------------------------
                         */

                        if (json != null) {

                            Log.d(
                                    TAG,
                                    "JSON RESPONSE RECEIVED"
                            );

                            Log.d(
                                    TAG,
                                    "JSON LENGTH = "
                                            + json.length()
                            );

                            Log.d(
                                    TAG,
                                    json
                            );

                        } else {

                            Log.e(
                                    TAG,
                                    "JSON RESPONSE = NULL"
                            );
                        }


                        /*
                         * ------------------------------------------------
                         * EMPTY RESPONSE
                         * ------------------------------------------------
                         */

                        if (
                                json == null
                                        ||
                                        json.trim().isEmpty()
                        ) {

                            sendResult(
                                    callback,
                                    new MasterDataSyncResult(
                                            false,
                                            "No response received from server."
                                    )
                            );

                            return;
                        }


                        /*
                         * ------------------------------------------------
                         * PARSE JSON
                         * ------------------------------------------------
                         */

                        Log.d(
                                TAG,
                                "Parsing JSON..."
                        );


                        MasterDataResponse response =
                                gson.fromJson(
                                        json,
                                        MasterDataResponse.class
                                );


                        /*
                         * ------------------------------------------------
                         * INVALID RESPONSE
                         * ------------------------------------------------
                         */

                        if (response == null) {

                            Log.e(
                                    TAG,
                                    "GSON RETURNED NULL"
                            );


                            sendResult(
                                    callback,
                                    new MasterDataSyncResult(
                                            false,
                                            "Unable to parse master data response."
                                    )
                            );

                            return;
                        }


                        /*
                         * ------------------------------------------------
                         * API STATUS
                         * ------------------------------------------------
                         */

                        Log.d(
                                TAG,
                                "API SUCCESS = "
                                        + response.success
                        );


                        Log.d(
                                TAG,
                                "API MESSAGE = "
                                        + response.message
                        );


                        /*
                         * ------------------------------------------------
                         * API FAILURE
                         * ------------------------------------------------
                         */

                        if (!response.success) {

                            String message =
                                    response.message != null
                                            &&
                                            !response.message
                                                    .trim()
                                                    .isEmpty()

                                            ? response.message

                                            : "Master data request failed.";


                            Log.e(
                                    TAG,
                                    "API FAILURE: "
                                            + message
                            );


                            sendResult(
                                    callback,
                                    new MasterDataSyncResult(
                                            false,
                                            message
                                    )
                            );

                            return;
                        }


                        /*
                         * ------------------------------------------------
                         * EMPTY DATA
                         * ------------------------------------------------
                         */

                        if (response.data == null) {

                            Log.e(
                                    TAG,
                                    "RESPONSE DATA IS NULL"
                            );


                            sendResult(
                                    callback,
                                    new MasterDataSyncResult(
                                            false,
                                            "Master data response is empty."
                                    )
                            );

                            return;
                        }


                        /*
                         * ------------------------------------------------
                         * MASTER DATA PAYLOAD
                         * ------------------------------------------------
                         */

                        MasterDataPayload data =
                                response.data;


                        /*
                         * ------------------------------------------------
                         * COUNT RECORDS
                         * ------------------------------------------------
                         */

                        int farms =
                                data.farms != null
                                        ? data.farms.size()
                                        : 0;


                        int sections =
                                data.sections != null
                                        ? data.sections.size()
                                        : 0;


                        int greenhouses =
                                data.greenhouses != null
                                        ? data.greenhouses.size()
                                        : 0;


                        int bays =
                                data.bays != null
                                        ? data.bays.size()
                                        : 0;


                        int beds =
                                data.beds != null
                                        ? data.beds.size()
                                        : 0;


                        int scanningPoints =
                                data.scanning_points != null
                                        ? data.scanning_points.size()
                                        : 0;


                        int scanningPointBeds =
                                data.scanning_point_beds != null
                                        ? data.scanning_point_beds.size()
                                        : 0;


                        int fcmTraps =
                                data.fcm_traps != null
                                        ? data.fcm_traps.size()
                                        : 0;


                        int insectTypes =
                                data.insect_types != null
                                        ? data.insect_types.size()
                                        : 0;


                        int observationTypes =
                                data.observation_types != null
                                        ? data.observation_types.size()
                                        : 0;


                        int scoreDefinitions =
                                data.score_definitions != null
                                        ? data.score_definitions.size()
                                        : 0;


                        /*
                         * ------------------------------------------------
                         * LOG COUNTS
                         * ------------------------------------------------
                         */

                        Log.d(
                                TAG,
                                "----------------------------------------"
                        );

                        Log.d(
                                TAG,
                                "MASTER DATA RECEIVED"
                        );

                        Log.d(
                                TAG,
                                "Farms = "
                                        + farms
                        );

                        Log.d(
                                TAG,
                                "Sections = "
                                        + sections
                        );

                        Log.d(
                                TAG,
                                "Greenhouses = "
                                        + greenhouses
                        );

                        Log.d(
                                TAG,
                                "Bays = "
                                        + bays
                        );

                        Log.d(
                                TAG,
                                "Beds = "
                                        + beds
                        );

                        Log.d(
                                TAG,
                                "Scanning Points = "
                                        + scanningPoints
                        );

                        Log.d(
                                TAG,
                                "Scanning Point Beds = "
                                        + scanningPointBeds
                        );

                        Log.d(
                                TAG,
                                "FCM Traps = "
                                        + fcmTraps
                        );

                        Log.d(
                                TAG,
                                "Insect Types = "
                                        + insectTypes
                        );

                        Log.d(
                                TAG,
                                "Observation Types = "
                                        + observationTypes
                        );

                        Log.d(
                                TAG,
                                "Score Definitions = "
                                        + scoreDefinitions
                        );

                        Log.d(
                                TAG,
                                "----------------------------------------"
                        );


                        /*
                         * ------------------------------------------------
                         * SAVE INTO ROOM
                         * ------------------------------------------------
                         */

                        Log.d(
                                TAG,
                                "Saving master data into Room..."
                        );

                        Log.d(
                                TAG,
                                "FCM Traps = "
                                        + fcmTraps
                        );

                        Log.d(
                                TAG,
                                "Insect Types = "
                                        + insectTypes
                        );


                        repository.syncMasterData(
                                response,
                                result -> {

                                    /*
                                     * Repository successfully completed.
                                     */

                                    if (
                                            result != null
                                                    &&
                                                    result.isSuccess()
                                    ) {

                                        Log.d(
                                                TAG,
                                                "ROOM SAVE SUCCESSFUL"
                                        );

                                        Log.d(
                                                TAG,
                                                "MASTER DATA AVAILABLE OFFLINE"
                                        );

                                    } else {

                                        Log.e(
                                                TAG,
                                                "ROOM SAVE FAILED"
                                        );
                                    }


                                    sendResult(
                                            callback,
                                            new MasterDataSyncResult(
                                                    result != null
                                                            &&
                                                            result.isSuccess(),

                                                    result != null
                                                            ? result.getMessage()
                                                            : "Unable to save master data.",

                                                    farms,
                                                    sections,
                                                    greenhouses,
                                                    bays,
                                                    beds,
                                                    scanningPoints,
                                                    scanningPointBeds,
                                                    fcmTraps,
                                                    insectTypes,
                                                    observationTypes,
                                                    scoreDefinitions
                                            )
                                    );
                                }
                        );


                    } catch (Exception e) {

                        Log.e(
                                TAG,
                                "MASTER DATA SYNC ERROR",
                                e
                        );


                        String message =
                                e.getMessage();


                        if (
                                message == null
                                        ||
                                        message.trim().isEmpty()
                        ) {

                            message =
                                    "Unable to synchronize master data.";
                        }


                        sendResult(
                                callback,
                                new MasterDataSyncResult(
                                        false,
                                        message
                                )
                        );
                    }
                }
        );
    }


    /*
     * ================================================================
     * SEND RESULT TO MAIN THREAD
     * ================================================================
     */

    private void sendResult(
            SyncCallback callback,
            MasterDataSyncResult result
    ) {

        if (callback == null) {

            return;
        }


        mainHandler.post(
                () ->
                        callback.onComplete(
                                result
                        )
        );
    }


    /*
     * ================================================================
     * CALLBACK
     * ================================================================
     */

    public interface SyncCallback {

        void onComplete(
                MasterDataSyncResult result
        );
    }


    /*
     * ================================================================
     * SHUTDOWN
     * ================================================================
     */

    public void shutdown() {

        executorService.shutdown();

        repository.shutdown();
    }
}