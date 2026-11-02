package com.aaagrowers.scoutingapp.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aaagrowers.scoutingapp.data.database.AppDatabase;
import com.aaagrowers.scoutingapp.data.database.dao.BayDao;
import com.aaagrowers.scoutingapp.data.database.dao.BedDao;
import com.aaagrowers.scoutingapp.data.database.dao.FarmDao;
import com.aaagrowers.scoutingapp.data.database.dao.FcmTrapDao;
import com.aaagrowers.scoutingapp.data.database.dao.GreenhouseDao;
import com.aaagrowers.scoutingapp.data.database.dao.InsectTypeDao;
import com.aaagrowers.scoutingapp.data.database.dao.ObservationTypeDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScanningPointBedDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScanningPointDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScoreDefinitionDao;
import com.aaagrowers.scoutingapp.data.database.dao.SectionDao;

import com.aaagrowers.scoutingapp.data.model.MasterDataPayload;
import com.aaagrowers.scoutingapp.data.model.MasterDataResponse;
import com.aaagrowers.scoutingapp.data.model.MasterDataSyncResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MasterDataRepository {

    private final AppDatabase database;

    private final FarmDao farmDao;
    private final SectionDao sectionDao;
    private final GreenhouseDao greenhouseDao;
    private final BayDao bayDao;
    private final BedDao bedDao;
    private final ScanningPointDao scanningPointDao;
    private final ScanningPointBedDao scanningPointBedDao;
    private final ObservationTypeDao observationTypeDao;
    private final ScoreDefinitionDao scoreDefinitionDao;
    private final FcmTrapDao fcmTrapDao;
    private final InsectTypeDao insectTypeDao;

    private final ExecutorService executorService;


    public MasterDataRepository(
            @NonNull Context context
    ) {

        database =
                AppDatabase.getInstance(
                        context.getApplicationContext()
                );


        farmDao =
                database.farmDao();

        sectionDao =
                database.sectionDao();

        greenhouseDao =
                database.greenhouseDao();

        bayDao =
                database.bayDao();

        bedDao =
                database.bedDao();

        scanningPointDao =
                database.scanningPointDao();

        scanningPointBedDao =
                database.scanningPointBedDao();

        observationTypeDao =
                database.observationTypeDao();

        scoreDefinitionDao =
                database.scoreDefinitionDao();

        fcmTrapDao =
                database.fcmTrapDao();

        insectTypeDao =
                database.insectTypeDao();


        executorService =
                Executors.newSingleThreadExecutor();
    }


    /*
     * ================================================================
     * SYNCHRONIZE MASTER DATA
     * ================================================================
     */

    public void syncMasterData(
            MasterDataResponse response,
            SyncCallback callback
    ) {

        executorService.execute(
                () -> {

                    try {

                        /*
                         * ------------------------------------------------
                         * VALIDATE RESPONSE
                         * ------------------------------------------------
                         */

                        if (response == null) {

                            callback.onComplete(
                                    new MasterDataSyncResult(
                                            false,
                                            "No response received."
                                    )
                            );

                            return;
                        }


                        if (!response.success) {

                            callback.onComplete(
                                    new MasterDataSyncResult(
                                            false,
                                            response.message != null
                                                    ? response.message
                                                    : "Master data request failed."
                                    )
                            );

                            return;
                        }


                        if (response.data == null) {

                            callback.onComplete(
                                    new MasterDataSyncResult(
                                            false,
                                            "Master data response is empty."
                                    )
                            );

                            return;
                        }


                        MasterDataPayload data =
                                response.data;


                        /*
                         * ------------------------------------------------
                         * TRANSACTION
                         * ------------------------------------------------
                         */

                        database.runInTransaction(
                                () -> {

                                    /*
                                     * IMPORTANT
                                     *
                                     * Delete children first.
                                     */

                                    scanningPointBedDao.deleteAll();

                                    scanningPointDao.deleteAll();

                                    bedDao.deleteAll();

                                    bayDao.deleteAll();

                                    greenhouseDao.deleteAll();

                                    sectionDao.deleteAll();

                                    farmDao.deleteAll();

                                    observationTypeDao.deleteAll();

                                    scoreDefinitionDao.deleteAll();

                                    fcmTrapDao.deleteAll();

                                    insectTypeDao.deleteAll();


                                    /*
                                     * ------------------------------------------------
                                     * INSERT HIERARCHY
                                     * ------------------------------------------------
                                     */

                                    if (data.farms != null) {

                                        farmDao.insertAll(
                                                data.farms
                                        );
                                    }


                                    if (data.sections != null) {

                                        sectionDao.insertAll(
                                                data.sections
                                        );
                                    }


                                    if (data.greenhouses != null) {

                                        greenhouseDao.insertAll(
                                                data.greenhouses
                                        );
                                    }


                                    if (data.bays != null) {

                                        bayDao.insertAll(
                                                data.bays
                                        );
                                    }


                                    if (data.beds != null) {

                                        bedDao.insertAll(
                                                data.beds
                                        );
                                    }


                                    if (data.scanning_points != null) {

                                        scanningPointDao.insertAll(
                                                data.scanning_points
                                        );
                                    }


                                    if (data.scanning_point_beds != null) {

                                        scanningPointBedDao.insertAll(
                                                data.scanning_point_beds
                                        );
                                    }


                                    /*
                                     * ------------------------------------------------
                                     * FCM TRAPS
                                     * ------------------------------------------------
                                     */

                                    if (data.fcm_traps != null) {

                                        fcmTrapDao.insertAll(
                                                data.fcm_traps
                                        );
                                    }


                                    /*
                                     * ------------------------------------------------
                                     * INSECT TYPES
                                     * ------------------------------------------------
                                     *
                                     * Used by General Moth traps.
                                     *
                                     * These are downloaded from the server
                                     * and stored locally so the scout can
                                     * select an insect even when offline.
                                     *
                                     * ------------------------------------------------
                                     */

                                    if (data.insect_types != null) {

                                        insectTypeDao.insertAll(
                                                data.insect_types
                                        );
                                    }


                                    /*
                                     * ------------------------------------------------
                                     * OBSERVATIONS
                                     * ------------------------------------------------
                                     */

                                    if (data.observation_types != null) {

                                        observationTypeDao.insertAll(
                                                data.observation_types
                                        );
                                    }


                                    /*
                                     * ------------------------------------------------
                                     * SCORE DEFINITIONS
                                     * ------------------------------------------------
                                     */

                                    if (data.score_definitions != null) {

                                        scoreDefinitionDao.insertAll(
                                                data.score_definitions
                                        );
                                    }
                                }
                        );


                        /*
                         * ------------------------------------------------
                         * SUCCESS
                         * ------------------------------------------------
                         */

                        callback.onComplete(
                                new MasterDataSyncResult(
                                        true,
                                        "Master data synchronized successfully."
                                )
                        );


                    } catch (Exception e) {

                        e.printStackTrace();


                        callback.onComplete(
                                new MasterDataSyncResult(
                                        false,
                                        e.getMessage() != null
                                                ? e.getMessage()
                                                : "Unable to synchronize master data."
                                )
                        );
                    }
                }
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
    }
}