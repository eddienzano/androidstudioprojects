package com.aaagrowers.scoutingapp.network;

public final class ApiConfig {

    private ApiConfig() {
        // Prevent instantiation
    }


    /*
     * ============================================================
     * BASE URL
     * ============================================================
     */

    public static final String BASE_URL =
           "https://www.aaagrowers.co.ke/scouts/api/";




    /*
     * ============================================================
     * AUTH ENDPOINTS
     * ============================================================
     */

    public static final String LOGIN_URL =
            BASE_URL + "auth/login.php";


    /*
     * ============================================================
     * MASTER DATA ENDPOINTS
     * ============================================================
     */

    public static final String MASTER_DATA_URL =
            BASE_URL + "sync/master_data.php";

    /*
     * ============================================================
     * SCOUTING SYNC
     * ============================================================
     */

    public static final String SCOUTING_SYNC_URL =
            BASE_URL + "sync/scouting.php";

    /*
     * ============================================================
     * TRAP OBSERVATION SYNC
     * ============================================================
     */

    public static final String TRAP_OBSERVATION_SYNC_URL =
            BASE_URL + "sync/trap_observations.php";
}