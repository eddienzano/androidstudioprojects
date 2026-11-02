package com.aaagrowers.scoutingapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.aaagrowers.scoutingapp.R;
import com.aaagrowers.scoutingapp.dashboard.DashboardActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText passwordInput;

    private TextView loginStatus;

    private Button loginButton;

    private ImageButton passwordToggle;

    private SessionManager sessionManager;

    private AuthRepository authRepository;

    private boolean passwordVisible = false;


    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);


        /*
         * ============================================================
         * LAYOUT
         * ============================================================
         */

        setContentView(
                R.layout.activity_login
        );


        /*
         * ============================================================
         * SESSION
         * ============================================================
         */

        sessionManager =
                new SessionManager(this);


        /*
         * ============================================================
         * CHECK EXISTING SESSION
         * ============================================================
         */

        if (sessionManager.isLoggedIn()) {

            openDashboard();

            return;
        }


        /*
         * ============================================================
         * AUTH REPOSITORY
         * ============================================================
         */

        authRepository =
                new AuthRepository();


        /*
         * ============================================================
         * FIND VIEWS
         * ============================================================
         */

        usernameInput =
                findViewById(
                        R.id.usernameInput
                );


        passwordInput =
                findViewById(
                        R.id.passwordInput
                );


        loginStatus =
                findViewById(
                        R.id.loginStatus
                );


        loginButton =
                findViewById(
                        R.id.loginButton
                );


        passwordToggle =
                findViewById(
                        R.id.passwordToggle
                );


        /*
         * ============================================================
         * PASSWORD TOGGLE
         * ============================================================
         */

        passwordToggle.setOnClickListener(
                view -> togglePassword()
        );


        /*
         * ============================================================
         * LOGIN BUTTON
         * ============================================================
         */

        loginButton.setOnClickListener(
                view -> attemptLogin()
        );
    }


    /*
     * ================================================================
     * ATTEMPT LOGIN
     * ================================================================
     */

    private void attemptLogin() {

        String username =
                usernameInput
                        .getText()
                        .toString()
                        .trim();


        String password =
                passwordInput
                        .getText()
                        .toString();


        /*
         * ============================================================
         * VALIDATION
         * ============================================================
         */

        if (username.isEmpty()) {

            showError(
                    "Please enter your username."
            );

            usernameInput.requestFocus();

            return;
        }


        if (password.isEmpty()) {

            showError(
                    "Please enter your password."
            );

            passwordInput.requestFocus();

            return;
        }


        /*
         * ============================================================
         * START LOGIN
         * ============================================================
         */

        setLoginLoading(true);


        authRepository.login(
                username,
                password,
                new AuthRepository.LoginCallback() {


                    @Override
                    public void onSuccess(
                            int userId,
                            String returnedUsername,
                            String fullName,
                            String role
                    ) {

                        /*
                         * ------------------------------------------------
                         * SAVE SESSION
                         * ------------------------------------------------
                         */

                        sessionManager.createSession(
                                userId,
                                returnedUsername,
                                fullName,
                                role
                        );


                        /*
                         * ------------------------------------------------
                         * SUCCESS MESSAGE
                         * ------------------------------------------------
                         */

                        showSuccess(
                                "Login successful."
                        );


                        /*
                         * ------------------------------------------------
                         * OPEN DASHBOARD
                         * ------------------------------------------------
                         */

                        loginButton.postDelayed(
                                LoginActivity.this::openDashboard,
                                300
                        );
                    }


                    @Override
                    public void onError(
                            String message
                    ) {

                        setLoginLoading(false);

                        showError(
                                message
                        );
                    }
                }
        );
    }


    /*
     * ================================================================
     * PASSWORD TOGGLE
     * ================================================================
     */

    private void togglePassword() {

        if (passwordVisible) {

            passwordInput.setInputType(
                    InputType.TYPE_CLASS_TEXT
                            |
                            InputType.TYPE_TEXT_VARIATION_PASSWORD
            );

            passwordVisible = false;

        } else {

            passwordInput.setInputType(
                    InputType.TYPE_CLASS_TEXT
                            |
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            );

            passwordVisible = true;
        }


        passwordInput.setSelection(
                passwordInput.length()
        );
    }


    /*
     * ================================================================
     * LOADING STATE
     * ================================================================
     */

    private void setLoginLoading(
            boolean loading
    ) {

        loginButton.setEnabled(
                !loading
        );


        usernameInput.setEnabled(
                !loading
        );


        passwordInput.setEnabled(
                !loading
        );


        passwordToggle.setEnabled(
                !loading
        );


        if (loading) {

            loginButton.setText(
                    "Signing in..."
            );

            loginStatus.setVisibility(
                    View.GONE
            );

        } else {

            loginButton.setText(
                    "Sign in     →"
            );
        }
    }


    /*
     * ================================================================
     * ERROR
     * ================================================================
     */

    private void showError(
            String message
    ) {

        loginStatus.setText(
                message
        );


        loginStatus.setTextColor(
                getColor(
                        android.R.color.holo_red_dark
                )
        );


        loginStatus.setVisibility(
                View.VISIBLE
        );
    }


    /*
     * ================================================================
     * SUCCESS
     * ================================================================
     */

    private void showSuccess(
            String message
    ) {

        loginStatus.setText(
                message
        );


        loginStatus.setTextColor(
                getColor(
                        android.R.color.holo_green_dark
                )
        );


        loginStatus.setVisibility(
                View.VISIBLE
        );
    }


    /*
     * ================================================================
     * OPEN DASHBOARD
     * ================================================================
     */

    private void openDashboard() {

        Intent intent =
                new Intent(
                        LoginActivity.this,
                        DashboardActivity.class
                );


        startActivity(intent);


        finish();
    }


    /*
     * ================================================================
     * CLEANUP
     * ================================================================
     */

    @Override
    protected void onDestroy() {

        if (authRepository != null) {

            authRepository.shutdown();
        }


        super.onDestroy();
    }
}