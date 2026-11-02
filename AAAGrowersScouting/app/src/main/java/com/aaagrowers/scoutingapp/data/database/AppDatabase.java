package com.aaagrowers.scoutingapp.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.aaagrowers.scoutingapp.data.database.dao.BayDao;
import com.aaagrowers.scoutingapp.data.database.dao.BedDao;
import com.aaagrowers.scoutingapp.data.database.dao.FarmDao;
import com.aaagrowers.scoutingapp.data.database.dao.FcmTrapDao;
import com.aaagrowers.scoutingapp.data.database.dao.GreenhouseDao;
import com.aaagrowers.scoutingapp.data.database.dao.InsectTypeDao;
import com.aaagrowers.scoutingapp.data.database.dao.ObservationTypeDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScanningPointBedDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScanningPointDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScoutingGpsTrackDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScoutingObservationDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScoutingRoundDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScoutingSeverePointDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScoreDefinitionDao;
import com.aaagrowers.scoutingapp.data.database.dao.SectionDao;
import com.aaagrowers.scoutingapp.data.database.dao.TrapObservationDao;
import com.aaagrowers.scoutingapp.data.database.dao.UserDao;

import com.aaagrowers.scoutingapp.data.database.entity.BayEntity;
import com.aaagrowers.scoutingapp.data.database.entity.BedEntity;
import com.aaagrowers.scoutingapp.data.database.entity.FarmEntity;
import com.aaagrowers.scoutingapp.data.database.entity.FcmTrapEntity;
import com.aaagrowers.scoutingapp.data.database.entity.GreenhouseEntity;
import com.aaagrowers.scoutingapp.data.database.entity.InsectTypeEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ObservationTypeEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScanningPointBedEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScanningPointEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScoutingGpsTrackEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScoutingObservationEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScoutingRoundEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScoutingSeverePointEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScoreDefinitionEntity;
import com.aaagrowers.scoutingapp.data.database.entity.SectionEntity;
import com.aaagrowers.scoutingapp.data.database.entity.TrapObservationEntity;
import com.aaagrowers.scoutingapp.data.database.entity.UserEntity;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;


/*
 * ============================================================================
 * SCOUTING APP
 * LOCAL ROOM DATABASE
 * ============================================================================
 *
 * DATABASE VERSION HISTORY
 *
 *   Version 1
 *      Initial scouting database
 *
 *   Version 2
 *      Added synchronization fields to scouting transaction tables
 *
 *   Version 3
 *      Added FCM trap master data
 *
 *   Version 4
 *      Added insect type master data
 *
 *   Version 5
 *      Added local trap observations
 *
 * ============================================================================
 */

@Database(
        entities = {

                /*
                 * ------------------------------------------------------------
                 * USER / MASTER DATA
                 * ------------------------------------------------------------
                 */

                UserEntity.class,
                FarmEntity.class,
                SectionEntity.class,
                GreenhouseEntity.class,
                BayEntity.class,
                BedEntity.class,

                /*
                 * ------------------------------------------------------------
                 * SCANNING / SCOUTING MASTER DATA
                 * ------------------------------------------------------------
                 */

                ScanningPointEntity.class,
                ScanningPointBedEntity.class,
                ObservationTypeEntity.class,
                ScoreDefinitionEntity.class,

                /*
                 * ------------------------------------------------------------
                 * SCOUTING TRANSACTION DATA
                 * ------------------------------------------------------------
                 */

                ScoutingRoundEntity.class,
                ScoutingObservationEntity.class,
                ScoutingSeverePointEntity.class,
                ScoutingGpsTrackEntity.class,

                /*
                 * ------------------------------------------------------------
                 * TRAP MASTER DATA
                 * ------------------------------------------------------------
                 */

                FcmTrapEntity.class,
                InsectTypeEntity.class,

                /*
                 * ------------------------------------------------------------
                 * TRAP MONITORING TRANSACTION DATA
                 * ------------------------------------------------------------
                 */

                TrapObservationEntity.class
        },

        /*
         * IMPORTANT:
         *
         * TrapObservationEntity was added in Version 5.
         */
        version = 6,

        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {


    /*
     * =========================================================================
     * DAO ACCESS
     * =========================================================================
     */

    public abstract UserDao userDao();

    public abstract FarmDao farmDao();

    public abstract SectionDao sectionDao();

    public abstract GreenhouseDao greenhouseDao();

    public abstract BayDao bayDao();

    public abstract BedDao bedDao();

    public abstract ScanningPointDao scanningPointDao();

    public abstract ScanningPointBedDao scanningPointBedDao();

    public abstract ObservationTypeDao observationTypeDao();

    public abstract ScoreDefinitionDao scoreDefinitionDao();

    public abstract ScoutingRoundDao scoutingRoundDao();

    public abstract ScoutingObservationDao scoutingObservationDao();

    public abstract ScoutingSeverePointDao scoutingSeverePointDao();

    public abstract ScoutingGpsTrackDao scoutingGpsTrackDao();

    public abstract FcmTrapDao fcmTrapDao();

    public abstract InsectTypeDao insectTypeDao();

    public abstract TrapObservationDao trapObservationDao();


    /*
     * =========================================================================
     * MIGRATION 1 → 2
     * =========================================================================
     *
     * Adds local synchronization tracking to scouting transaction tables.
     *
     * =========================================================================
     */

    public static final Migration MIGRATION_1_2 =
            new Migration(1, 2) {

                @Override
                public void migrate(
                        SupportSQLiteDatabase database
                ) {

                    /*
                     * ---------------------------------------------------------
                     * SCOUTING ROUNDS
                     * ---------------------------------------------------------
                     */

                    database.execSQL(
                            "ALTER TABLE scouting_rounds " +
                                    "ADD COLUMN sync_status TEXT NOT NULL " +
                                    "DEFAULT 'pending'"
                    );

                    database.execSQL(
                            "ALTER TABLE scouting_rounds " +
                                    "ADD COLUMN sync_error TEXT"
                    );


                    /*
                     * ---------------------------------------------------------
                     * SCOUTING OBSERVATIONS
                     * ---------------------------------------------------------
                     */

                    database.execSQL(
                            "ALTER TABLE scouting_observations " +
                                    "ADD COLUMN sync_status TEXT NOT NULL " +
                                    "DEFAULT 'pending'"
                    );

                    database.execSQL(
                            "ALTER TABLE scouting_observations " +
                                    "ADD COLUMN sync_error TEXT"
                    );


                    /*
                     * ---------------------------------------------------------
                     * SCOUTING SEVERE POINTS
                     * ---------------------------------------------------------
                     */

                    database.execSQL(
                            "ALTER TABLE scouting_severe_points " +
                                    "ADD COLUMN sync_status TEXT NOT NULL " +
                                    "DEFAULT 'pending'"
                    );

                    database.execSQL(
                            "ALTER TABLE scouting_severe_points " +
                                    "ADD COLUMN sync_error TEXT"
                    );


                    /*
                     * ---------------------------------------------------------
                     * SCOUTING GPS TRACKS
                     * ---------------------------------------------------------
                     */

                    database.execSQL(
                            "ALTER TABLE scouting_gps_tracks " +
                                    "ADD COLUMN sync_status TEXT NOT NULL " +
                                    "DEFAULT 'pending'"
                    );

                    database.execSQL(
                            "ALTER TABLE scouting_gps_tracks " +
                                    "ADD COLUMN sync_error TEXT"
                    );
                }
            };


    /*
     * =========================================================================
     * MIGRATION 2 → 3
     * =========================================================================
     *
     * Adds FCM trap master data.
     *
     * =========================================================================
     */

    public static final Migration MIGRATION_2_3 =
            new Migration(2, 3) {

                @Override
                public void migrate(
                        SupportSQLiteDatabase database
                ) {

                    /*
                     * ---------------------------------------------------------
                     * FCM TRAPS
                     * ---------------------------------------------------------
                     */

                    database.execSQL(
                            "CREATE TABLE IF NOT EXISTS fcm_traps (" +

                                    "id INTEGER NOT NULL, " +

                                    "trap_code TEXT, " +

                                    "qr_code TEXT, " +

                                    "bay_id INTEGER NOT NULL, " +

                                    "trap_type TEXT, " +

                                    "latitude REAL, " +

                                    "longitude REAL, " +

                                    "installed_at TEXT, " +

                                    "status TEXT, " +

                                    "notes TEXT, " +

                                    "created_at TEXT, " +

                                    "updated_at TEXT, " +

                                    "PRIMARY KEY(id)" +

                                    ")"
                    );


                    /*
                     * ---------------------------------------------------------
                     * UNIQUE TRAP CODE
                     * ---------------------------------------------------------
                     */

                    database.execSQL(
                            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                                    "index_fcm_traps_trap_code " +
                                    "ON fcm_traps(trap_code)"
                    );


                    /*
                     * ---------------------------------------------------------
                     * UNIQUE QR CODE
                     * ---------------------------------------------------------
                     */

                    database.execSQL(
                            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                                    "index_fcm_traps_qr_code " +
                                    "ON fcm_traps(qr_code)"
                    );


                    /*
                     * ---------------------------------------------------------
                     * BAY INDEX
                     * ---------------------------------------------------------
                     */

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS " +
                                    "index_fcm_traps_bay_id " +
                                    "ON fcm_traps(bay_id)"
                    );


                    /*
                     * ---------------------------------------------------------
                     * STATUS INDEX
                     * ---------------------------------------------------------
                     */

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS " +
                                    "index_fcm_traps_status " +
                                    "ON fcm_traps(status)"
                    );
                }
            };


    /*
     * =========================================================================
     * MIGRATION 3 → 4
     * =========================================================================
     *
     * Adds insect type master data.
     *
     * Used by General Moth traps.
     *
     * =========================================================================
     */

    public static final Migration MIGRATION_3_4 =
            new Migration(3, 4) {

                @Override
                public void migrate(
                        SupportSQLiteDatabase database
                ) {

                    /*
                     * ---------------------------------------------------------
                     * INSECT TYPES
                     * ---------------------------------------------------------
                     */

                    database.execSQL(
                            "CREATE TABLE IF NOT EXISTS insect_types (" +

                                    "id INTEGER NOT NULL, " +

                                    "code TEXT, " +

                                    "name TEXT, " +

                                    "scientific_name TEXT, " +

                                    "description TEXT, " +

                                    "active INTEGER NOT NULL, " +

                                    "sort_order INTEGER NOT NULL, " +

                                    "created_at TEXT, " +

                                    "updated_at TEXT, " +

                                    "PRIMARY KEY(id)" +

                                    ")"
                    );


                    /*
                     * ---------------------------------------------------------
                     * UNIQUE CODE INDEX
                     * ---------------------------------------------------------
                     */

                    database.execSQL(
                            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                                    "index_insect_types_code " +
                                    "ON insect_types(code)"
                    );


                    /*
                     * ---------------------------------------------------------
                     * ACTIVE INDEX
                     * ---------------------------------------------------------
                     */

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS " +
                                    "index_insect_types_active " +
                                    "ON insect_types(active)"
                    );


                    /*
                     * ---------------------------------------------------------
                     * SORT ORDER INDEX
                     * ---------------------------------------------------------
                     */

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS " +
                                    "index_insect_types_sort_order " +
                                    "ON insect_types(sort_order)"
                    );
                }
            };


    /*
     * =========================================================================
     * MIGRATION 4 → 5
     * =========================================================================
     *
     * Adds local trap monitoring observations.
     *
     * These observations are captured offline first and marked:
     *
     *      sync_status = pending
     *
     * The existing "Send to Database" synchronization process can later
     * upload these records to the server.
     *
     * =========================================================================
     */

    public static final Migration MIGRATION_4_5 =
            new Migration(4, 5) {

                @Override
                public void migrate(
                        SupportSQLiteDatabase database
                ) {

                    /*
                     * ---------------------------------------------------------
                     * TRAP OBSERVATIONS
                     * ---------------------------------------------------------
                     */

                    database.execSQL(
                            "CREATE TABLE IF NOT EXISTS trap_observations (" +

                                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +

                                    "trap_id INTEGER NOT NULL, " +

                                    "trap_code TEXT, " +

                                    "trap_type TEXT, " +

                                    "insect_type_id INTEGER, " +

                                    "insect_type_name TEXT, " +

                                    "moth_count INTEGER NOT NULL, " +

                                    "trend TEXT, " +

                                    "comments TEXT, " +

                                    "photo_path TEXT, " +

                                    "latitude REAL, " +

                                    "longitude REAL, " +

                                    "observed_at TEXT, " +

                                    "user_id INTEGER NOT NULL, " +

                                    "sync_status TEXT NOT NULL " +
                                    "DEFAULT 'pending', " +

                                    "sync_error TEXT, " +

                                    "created_at TEXT" +

                                    ")"
                    );


                    /*
                     * ---------------------------------------------------------
                     * TRAP ID INDEX
                     * ---------------------------------------------------------
                     *
                     * Allows quick retrieval of observations belonging
                     * to a particular trap.
                     */

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS " +
                                    "index_trap_observations_trap_id " +
                                    "ON trap_observations(trap_id)"
                    );


                    /*
                     * ---------------------------------------------------------
                     * SYNC STATUS INDEX
                     * ---------------------------------------------------------
                     *
                     * Important for the future "Send to Database" process.
                     *
                     * Example:
                     *
                     *      SELECT * FROM trap_observations
                     *      WHERE sync_status = 'pending'
                     *
                     */

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS " +
                                    "index_trap_observations_sync_status " +
                                    "ON trap_observations(sync_status)"
                    );


                    /*
                     * ---------------------------------------------------------
                     * OBSERVATION DATE INDEX
                     * ---------------------------------------------------------
                     *
                     * Useful for displaying history and future reporting.
                     */

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS " +
                                    "index_trap_observations_observed_at " +
                                    "ON trap_observations(observed_at)"
                    );
                }
            };

    static final Migration MIGRATION_5_6 =
            new Migration(5, 6) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase database) {

                    database.execSQL(
                            "ALTER TABLE trap_observations " +
                                    "ADD COLUMN client_uuid TEXT NOT NULL DEFAULT ''"
                    );

                    database.execSQL(
                            "ALTER TABLE trap_observations " +
                                    "ADD COLUMN gps_accuracy REAL"
                    );

                    database.execSQL(
                            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                                    "index_trap_observations_client_uuid " +
                                    "ON trap_observations(client_uuid)"
                    );
                }
            };


    /*
     * =========================================================================
     * SINGLETON INSTANCE
     * =========================================================================
     */

    private static volatile AppDatabase INSTANCE;


    public static AppDatabase getInstance(
            Context context
    ) {

        if (INSTANCE == null) {

            synchronized (AppDatabase.class) {

                if (INSTANCE == null) {

                    INSTANCE =
                            Room.databaseBuilder(
                                            context.getApplicationContext(),
                                            AppDatabase.class,
                                            "scouting_database"
                                    )

                                    /*
                                     * ------------------------------------------------
                                     * REAL DATABASE MIGRATIONS
                                     * ------------------------------------------------
                                     *
                                     * DO NOT use:
                                     *
                                     * fallbackToDestructiveMigration()
                                     *
                                     * because doing so would delete local
                                     * offline scouting data when the schema
                                     * changes.
                                     * ------------------------------------------------
                                     */

                                    .addMigrations(
                                            MIGRATION_1_2,
                                            MIGRATION_2_3,
                                            MIGRATION_3_4,
                                            MIGRATION_4_5,
                                            MIGRATION_5_6
                                    )

                                    .build();
                }
            }
        }

        return INSTANCE;
    }
}