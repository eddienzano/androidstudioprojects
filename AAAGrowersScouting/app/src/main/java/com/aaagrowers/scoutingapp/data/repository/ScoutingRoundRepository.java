package com.aaagrowers.scoutingapp.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aaagrowers.scoutingapp.data.database.AppDatabase;
import com.aaagrowers.scoutingapp.data.database.dao.ScoutingRoundDao;
import com.aaagrowers.scoutingapp.data.database.entity.ScoutingRoundEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScoutingRoundRepository {

    /*
     * ================================================================
     * DATABASE
     * ================================================================
     */

    private final AppDatabase database;

    private final ScoutingRoundDao scoutingRoundDao;

    private final ExecutorService executorService;


    /*
     * ================================================================
     * CONSTRUCTOR
     * ================================================================
     */

    public ScoutingRoundRepository(
            @NonNull Context context
    ) {

        database =
                AppDatabase.getInstance(
                        context.getApplicationContext()
                );


        scoutingRoundDao =
                database.scoutingRoundDao();


        executorService =
                Executors.newSingleThreadExecutor();
    }


    /*
     * ================================================================
     * CREATE SCOUTING ROUND
     * ================================================================
     */

    public void createRound(
            int scoutId,
            int sectionId,
            CreateRoundCallback callback
    ) {

        executorService.execute(
                () -> {

                    try {

                        /*
                         * ------------------------------------------------
                         * CHECK FOR EXISTING OPEN ROUND
                         * ------------------------------------------------
                         */

                        List<ScoutingRoundEntity> openRounds =
                                scoutingRoundDao.getOpenRoundsForScout(
                                        scoutId
                                );


                        if (
                                openRounds != null
                                        &&
                                        !openRounds.isEmpty()
                        ) {

                            ScoutingRoundEntity existingRound =
                                    openRounds.get(0);


                            callback.onComplete(
                                    true,
                                    existingRound,
                                    "An open scouting round already exists."
                            );

                            return;
                        }


                        /*
                         * ------------------------------------------------
                         * CREATE NEW ROUND
                         * ------------------------------------------------
                         */

                        ScoutingRoundEntity round =
                                new ScoutingRoundEntity();


                        round.client_uuid =
                                UUID.randomUUID()
                                        .toString();


                        round.scout_id =
                                scoutId;


                        round.section_id =
                                sectionId;


                        String now =
                                getCurrentDateTime();


                        round.round_date =
                                getCurrentDate();


                        round.started_at =
                                now;


                        round.completed_at =
                                null;


                        round.status =
                                "open";


                        round.notes =
                                null;


                        round.created_at =
                                now;


                        round.updated_at =
                                now;


                        /*
                         * ------------------------------------------------
                         * INSERT INTO ROOM
                         * ------------------------------------------------
                         */

                        long localId =
                                scoutingRoundDao.insert(
                                        round
                                );


                        round.id =
                                localId;


                        /*
                         * ------------------------------------------------
                         * RETURN SUCCESS
                         * ------------------------------------------------
                         */

                        callback.onComplete(
                                true,
                                round,
                                "Scouting round started successfully."
                        );


                    } catch (Exception e) {

                        e.printStackTrace();


                        callback.onComplete(
                                false,
                                null,
                                e.getMessage() != null
                                        ? e.getMessage()
                                        : "Unable to start scouting round."
                        );
                    }
                }
        );
    }


    /*
     * ================================================================
     * GET OPEN ROUND
     * ================================================================
     */

    public void getOpenRound(
            int scoutId,
            RoundCallback callback
    ) {

        executorService.execute(
                () -> {

                    try {

                        List<ScoutingRoundEntity> rounds =
                                scoutingRoundDao.getOpenRoundsForScout(
                                        scoutId
                                );


                        if (
                                rounds != null
                                        &&
                                        !rounds.isEmpty()
                        ) {

                            callback.onComplete(
                                    rounds.get(0)
                            );

                        } else {

                            callback.onComplete(
                                    null
                            );
                        }


                    } catch (Exception e) {

                        e.printStackTrace();

                        callback.onComplete(
                                null
                        );
                    }
                }
        );
    }


    /*
     * ================================================================
     * GET ROUND BY ID
     * ================================================================
     */

    public void getRoundById(
            long roundId,
            RoundCallback callback
    ) {

        executorService.execute(
                () -> {

                    try {

                        ScoutingRoundEntity round =
                                scoutingRoundDao.getById(
                                        roundId
                                );


                        callback.onComplete(
                                round
                        );


                    } catch (Exception e) {

                        e.printStackTrace();

                        callback.onComplete(
                                null
                        );
                    }
                }
        );
    }


    /*
     * ================================================================
     * COMPLETE ROUND
     * ================================================================
     */

    public void completeRound(
            long roundId,
            String notes,
            CompleteRoundCallback callback
    ) {

        executorService.execute(
                () -> {

                    try {

                        ScoutingRoundEntity round =
                                scoutingRoundDao.getById(
                                        roundId
                                );


                        if (round == null) {

                            callback.onComplete(
                                    false,
                                    "Scouting round was not found."
                            );

                            return;
                        }


                        String now =
                                getCurrentDateTime();


                        round.status =
                                "completed";


                        round.completed_at =
                                now;


                        round.notes =
                                notes;


                        round.updated_at =
                                now;


                        scoutingRoundDao.update(
                                round
                        );


                        callback.onComplete(
                                true,
                                "Scouting round completed successfully."
                        );


                    } catch (Exception e) {

                        e.printStackTrace();


                        callback.onComplete(
                                false,
                                e.getMessage() != null
                                        ? e.getMessage()
                                        : "Unable to complete scouting round."
                        );
                    }
                }
        );
    }


    /*
     * ================================================================
     * GET ALL ROUNDS
     * ================================================================
     */

    public void getAllRounds(
            RoundsCallback callback
    ) {

        executorService.execute(
                () -> {

                    try {

                        List<ScoutingRoundEntity> rounds =
                                scoutingRoundDao.getAll();


                        callback.onComplete(
                                rounds
                        );


                    } catch (Exception e) {

                        e.printStackTrace();

                        callback.onComplete(
                                null
                        );
                    }
                }
        );
    }


    /*
     * ================================================================
     * DATE
     * ================================================================
     */

    private String getCurrentDate() {

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                );


        return formatter.format(
                new Date()
        );
    }


    /*
     * ================================================================
     * DATETIME
     * ================================================================
     */

    private String getCurrentDateTime() {

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                );


        return formatter.format(
                new Date()
        );
    }


    /*
     * ================================================================
     * CALLBACKS
     * ================================================================
     */

    public interface CreateRoundCallback {

        void onComplete(
                boolean success,
                ScoutingRoundEntity round,
                String message
        );
    }


    public interface RoundCallback {

        void onComplete(
                ScoutingRoundEntity round
        );
    }


    public interface RoundsCallback {

        void onComplete(
                List<ScoutingRoundEntity> rounds
        );
    }


    public interface CompleteRoundCallback {

        void onComplete(
                boolean success,
                String message
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