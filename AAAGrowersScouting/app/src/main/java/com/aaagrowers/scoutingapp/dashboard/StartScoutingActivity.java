package com.aaagrowers.scoutingapp.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aaagrowers.scoutingapp.R;
import com.aaagrowers.scoutingapp.auth.SessionManager;
import com.aaagrowers.scoutingapp.data.database.AppDatabase;
import com.aaagrowers.scoutingapp.data.database.dao.SectionDao;
import com.aaagrowers.scoutingapp.data.database.entity.ScoutingRoundEntity;
import com.aaagrowers.scoutingapp.data.database.entity.SectionEntity;
import com.aaagrowers.scoutingapp.data.repository.ScoutingRoundRepository;
import com.aaagrowers.scoutingapp.scouting.ScoutingActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StartScoutingActivity extends AppCompatActivity {

    /*
     * ================================================================
     * SESSION
     * ================================================================
     */

    private SessionManager sessionManager;


    /*
     * ================================================================
     * DATABASE
     * ================================================================
     */

    private SectionDao sectionDao;

    private ExecutorService sectionExecutor;


    /*
     * ================================================================
     * REPOSITORY
     * ================================================================
     */

    private ScoutingRoundRepository roundRepository;


    /*
     * ================================================================
     * VIEWS
     * ================================================================
     */

    private TextView scoutName;

    private TextView sectionStatus;

    private Spinner sectionSpinner;

    private Button startButton;

    private Button backButton;


    /*
     * ================================================================
     * DATA
     * ================================================================
     */

    private final List<SectionEntity> sections =
            new ArrayList<>();


    /*
     * ================================================================
     * ON CREATE
     * ================================================================
     */

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);


        setContentView(
                R.layout.activity_start_scouting
        );


        /*
         * ------------------------------------------------------------
         * SESSION
         * ------------------------------------------------------------
         */

        sessionManager =
                new SessionManager(this);


        if (!sessionManager.isLoggedIn()) {

            finish();

            return;
        }


        /*
         * ------------------------------------------------------------
         * DATABASE
         * ------------------------------------------------------------
         */

        AppDatabase database =
                AppDatabase.getInstance(
                        this
                );


        sectionDao =
                database.sectionDao();


        sectionExecutor =
                Executors.newSingleThreadExecutor();


        /*
         * ------------------------------------------------------------
         * REPOSITORY
         * ------------------------------------------------------------
         */

        roundRepository =
                new ScoutingRoundRepository(
                        this
                );


        /*
         * ------------------------------------------------------------
         * VIEWS
         * ------------------------------------------------------------
         */

        scoutName =
                findViewById(
                        R.id.scoutName
                );


        sectionStatus =
                findViewById(
                        R.id.sectionStatus
                );


        sectionSpinner =
                findViewById(
                        R.id.sectionSpinner
                );


        startButton =
                findViewById(
                        R.id.startScoutingButton
                );


        backButton =
                findViewById(
                        R.id.backButton
                );


        /*
         * ------------------------------------------------------------
         * DISPLAY SCOUT
         * ------------------------------------------------------------
         */

        String name =
                sessionManager.getFullName();


        if (
                name == null
                        ||
                        name.trim().isEmpty()
        ) {

            scoutName.setText(
                    "Field Scout"
            );

        } else {

            scoutName.setText(
                    name
            );
        }


        /*
         * ------------------------------------------------------------
         * INITIAL STATE
         * ------------------------------------------------------------
         */

        startButton.setEnabled(
                false
        );


        /*
         * ------------------------------------------------------------
         * LOAD SECTIONS
         * ------------------------------------------------------------
         */

        loadSections();


        /*
         * ------------------------------------------------------------
         * START
         * ------------------------------------------------------------
         */

        startButton.setOnClickListener(
                view -> startScouting()
        );


        /*
         * ------------------------------------------------------------
         * BACK
         * ------------------------------------------------------------
         */

        backButton.setOnClickListener(
                view -> finish()
        );
    }


    /*
     * ================================================================
     * LOAD SECTIONS
     * ================================================================
     */

    private void loadSections() {

        sectionStatus.setText(
                "Loading sections..."
        );


        sectionExecutor.execute(
                () -> {

                    try {

                        List<SectionEntity> result =
                                sectionDao.getAll();


                        runOnUiThread(
                                () -> {

                                    sections.clear();


                                    if (
                                            result != null
                                                    &&
                                                    !result.isEmpty()
                                    ) {

                                        sections.addAll(
                                                result
                                        );


                                        List<String> names =
                                                new ArrayList<>();


                                        for (
                                                SectionEntity section
                                                : sections
                                        ) {

                                            String displayName =
                                                    section.section_code
                                                            + " - "
                                                            + section.section_name;


                                            names.add(
                                                    displayName
                                            );
                                        }


                                        ArrayAdapter<String> adapter =
                                                new ArrayAdapter<>(
                                                        this,
                                                        android.R.layout.simple_spinner_item,
                                                        names
                                                );


                                        adapter.setDropDownViewResource(
                                                android.R.layout.simple_spinner_dropdown_item
                                        );


                                        sectionSpinner.setAdapter(
                                                adapter
                                        );


                                        sectionStatus.setText(
                                                sections.size()
                                                        + " sections available offline."
                                        );


                                        startButton.setEnabled(
                                                true
                                        );

                                    } else {

                                        sectionStatus.setText(
                                                "No sections found. Synchronize master data first."
                                        );


                                        startButton.setEnabled(
                                                false
                                        );
                                    }
                                }
                        );


                    } catch (Exception e) {

                        e.printStackTrace();


                        runOnUiThread(
                                () -> {

                                    sectionStatus.setText(
                                            "Unable to load sections."
                                    );


                                    Toast.makeText(
                                            this,
                                            "Unable to load sections.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                        );
                    }
                }
        );
    }


    /*
     * ================================================================
     * START SCOUTING
     * ================================================================
     */

    private void startScouting() {

        if (sections.isEmpty()) {

            Toast.makeText(
                    this,
                    "No sections are available.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        int position =
                sectionSpinner.getSelectedItemPosition();


        if (
                position < 0
                        ||
                        position >= sections.size()
        ) {

            Toast.makeText(
                    this,
                    "Please select a section.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        SectionEntity selectedSection =
                sections.get(
                        position
                );


        int scoutId =
                sessionManager.getUserId();


        if (scoutId <= 0) {

            Toast.makeText(
                    this,
                    "Invalid scout session.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        /*
         * ------------------------------------------------------------
         * DISABLE BUTTON
         * ------------------------------------------------------------
         */

        startButton.setEnabled(
                false
        );


        startButton.setText(
                "STARTING..."
        );


        sectionStatus.setText(
                "Creating scouting round..."
        );


        /*
         * ------------------------------------------------------------
         * CREATE LOCAL ROUND
         * ------------------------------------------------------------
         */

        roundRepository.createRound(
                scoutId,
                selectedSection.id,
                (
                        success,
                        round,
                        message
                ) -> {

                    runOnUiThread(
                            () -> {

                                if (
                                        success
                                                &&
                                                round != null
                                ) {

                                    openScoutingActivity(
                                            round,
                                            selectedSection
                                    );

                                } else {

                                    startButton.setEnabled(
                                            true
                                    );


                                    startButton.setText(
                                            "START SCOUTING"
                                    );


                                    sectionStatus.setText(
                                            message != null
                                                    ? message
                                                    : "Unable to start scouting."
                                    );


                                    Toast.makeText(
                                            this,
                                            message != null
                                                    ? message
                                                    : "Unable to start scouting.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }
                    );
                }
        );
    }


    /*
     * ================================================================
     * OPEN SCOUTING
     * ================================================================
     */

    private void openScoutingActivity(
            ScoutingRoundEntity round,
            SectionEntity section
    ) {

        Intent intent =
                new Intent(
                        StartScoutingActivity.this,
                        ScoutingActivity.class
                );


        intent.putExtra(
                "round_id",
                round.id
        );


        intent.putExtra(
                "section_id",
                section.id
        );


        intent.putExtra(
                "section_name",
                section.section_name
        );


        intent.putExtra(
                "section_code",
                section.section_code
        );


        startActivity(
                intent
        );


        finish();
    }


    /*
     * ================================================================
     * ON DESTROY
     * ================================================================
     */

    @Override
    protected void onDestroy() {

        if (sectionExecutor != null) {

            sectionExecutor.shutdown();
        }


        if (roundRepository != null) {

            roundRepository.shutdown();
        }


        super.onDestroy();
    }
}