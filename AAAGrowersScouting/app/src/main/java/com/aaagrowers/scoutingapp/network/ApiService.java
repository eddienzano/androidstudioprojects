package com.aaagrowers.scoutingapp.network;

/**
 * Central definition of API endpoints used by the Android application.
 *
 * ApiConfig owns the base URL.
 * ApiService owns the individual API endpoints.
 *
 * This keeps URLs out of Activities and Repositories.
 */
public final class ApiService {

    private ApiService() {
        // Prevent instantiation.
    }


    /*
     * ============================================================
     * AUTHENTICATION
     * ============================================================
     */

    public static String loginUrl() {

        return ApiConfig.BASE_URL
                + "auth/login.php";
    }


    /*
     * ============================================================
     * FUTURE ENDPOINTS
     * ============================================================
     *
     * We will add these as we reach the relevant milestones.
     *
     * Example:
     *
     * public static String syncUrl() {
     *
     *     return ApiConfig.BASE_URL
     *             + "sync/initial.php";
     * }
     *
     */


}