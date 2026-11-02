package com.aaagrowers.scoutingapp.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME =
            "aaa_scouting_session";

    private static final String KEY_LOGGED_IN =
            "logged_in";

    private static final String KEY_USER_ID =
            "user_id";

    private static final String KEY_USERNAME =
            "username";

    private static final String KEY_FULL_NAME =
            "full_name";

    private static final String KEY_ROLE =
            "role";


    private final SharedPreferences preferences;


    public SessionManager(Context context) {

        preferences =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREF_NAME,
                                Context.MODE_PRIVATE
                        );
    }


    public void createSession(
            int userId,
            String username,
            String fullName,
            String role
    ) {

        preferences.edit()

                .putBoolean(
                        KEY_LOGGED_IN,
                        true
                )

                .putInt(
                        KEY_USER_ID,
                        userId
                )

                .putString(
                        KEY_USERNAME,
                        username
                )

                .putString(
                        KEY_FULL_NAME,
                        fullName
                )

                .putString(
                        KEY_ROLE,
                        role
                )

                .apply();
    }


    public boolean isLoggedIn() {

        return preferences.getBoolean(
                KEY_LOGGED_IN,
                false
        );
    }


    public int getUserId() {

        return preferences.getInt(
                KEY_USER_ID,
                0
        );
    }


    public String getUsername() {

        return preferences.getString(
                KEY_USERNAME,
                ""
        );
    }


    public String getFullName() {

        return preferences.getString(
                KEY_FULL_NAME,
                ""
        );
    }


    public String getRole() {

        return preferences.getString(
                KEY_ROLE,
                ""
        );
    }


    public void logout() {

        preferences.edit()
                .clear()
                .apply();
    }
}