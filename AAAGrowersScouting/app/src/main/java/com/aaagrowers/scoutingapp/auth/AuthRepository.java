package com.aaagrowers.scoutingapp.auth;

import android.os.Handler;
import android.os.Looper;

import com.aaagrowers.scoutingapp.network.ApiClient;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthRepository {

    private final ApiClient apiClient;

    private final ExecutorService executorService;

    private final Handler mainHandler;


    /*
     * ============================================================
     * CALLBACK
     * ============================================================
     */

    public interface LoginCallback {

        void onSuccess(
                int userId,
                String username,
                String fullName,
                String role
        );


        void onError(
                String message
        );
    }


    /*
     * ============================================================
     * CONSTRUCTOR
     * ============================================================
     */

    public AuthRepository() {

        apiClient =
                new ApiClient();

        executorService =
                Executors.newSingleThreadExecutor();

        mainHandler =
                new Handler(
                        Looper.getMainLooper()
                );
    }


    /*
     * ============================================================
     * LOGIN
     * ============================================================
     */

    public void login(
            String username,
            String password,
            LoginCallback callback
    ) {

        executorService.execute(
                () -> {

                    try {

                        /*
                         * ------------------------------------------------
                         * CALL PHP API
                         * ------------------------------------------------
                         */

                        String response =
                                apiClient.login(
                                        username,
                                        password
                                );


                        /*
                         * ------------------------------------------------
                         * PARSE JSON
                         * ------------------------------------------------
                         */

                        JSONObject json =
                                new JSONObject(
                                        response
                                );


                        boolean success =
                                json.optBoolean(
                                        "success",
                                        false
                                );


                        /*
                         * ------------------------------------------------
                         * LOGIN FAILED
                         * ------------------------------------------------
                         */

                        if (!success) {

                            String message =
                                    json.optString(
                                            "message",
                                            "Invalid username or password."
                                    );


                            mainHandler.post(
                                    () ->
                                            callback.onError(
                                                    message
                                            )
                            );

                            return;
                        }


                        /*
                         * ------------------------------------------------
                         * USER OBJECT
                         * ------------------------------------------------
                         */

                        JSONObject user =
                                json.optJSONObject(
                                        "user"
                                );


                        if (user == null) {

                            mainHandler.post(
                                    () ->
                                            callback.onError(
                                                    "Invalid server response."
                                            )
                            );

                            return;
                        }


                        /*
                         * ------------------------------------------------
                         * USER DATA
                         * ------------------------------------------------
                         */

                        int userId =
                                user.optInt(
                                        "id",
                                        0
                                );


                        String returnedUsername =
                                user.optString(
                                        "username",
                                        ""
                                );


                        String fullName =
                                user.optString(
                                        "full_name",
                                        ""
                                );


                        String role =
                                user.optString(
                                        "role",
                                        ""
                                );


                        /*
                         * ------------------------------------------------
                         * VALIDATE USER DATA
                         * ------------------------------------------------
                         */

                        if (userId <= 0
                                ||
                                returnedUsername.isEmpty()) {

                            mainHandler.post(
                                    () ->
                                            callback.onError(
                                                    "Invalid user information received."
                                            )
                            );

                            return;
                        }


                        /*
                         * ------------------------------------------------
                         * SUCCESS
                         * ------------------------------------------------
                         */

                        mainHandler.post(
                                () ->
                                        callback.onSuccess(
                                                userId,
                                                returnedUsername,
                                                fullName,
                                                role
                                        )
                        );


                    } catch (Exception e) {

                        /*
                         * ------------------------------------------------
                         * CONNECTION / SERVER ERROR
                         * ------------------------------------------------
                         */

                        String message =
                                getErrorMessage(e);


                        mainHandler.post(
                                () ->
                                        callback.onError(
                                                message
                                        )
                        );
                    }
                }
        );
    }


    /*
     * ============================================================
     * ERROR MESSAGE
     * ============================================================
     */

    private String getErrorMessage(
            Exception exception
    ) {

        String message =
                exception.getMessage();


        if (message == null
                ||
                message.trim().isEmpty()) {

            return "Unable to connect to the server.";
        }


        /*
         * Don't expose technical HTTP errors
         * directly to the scout.
         */

        if (
                message.contains("Failed to connect")
                        ||
                        message.contains("Connection refused")
                        ||
                        message.contains("timed out")
                        ||
                        message.contains("Unable to resolve")
        ) {

            return "Unable to connect to the scouting server.";
        }


        return message;
    }


    /*
     * ============================================================
     * SHUTDOWN
     * ============================================================
     */

    public void shutdown() {

        executorService.shutdown();
    }
}