package com.aaagrowers.scoutingapp.data.model;

public class MasterDataSyncResult {

    private final boolean success;

    private final String message;

    private final int farms;

    private final int sections;

    private final int greenhouses;

    private final int bays;

    private final int beds;

    private final int scanningPoints;

    private final int scanningPointBeds;

    private final int fcmTraps;

    private final int insectTypes;

    private final int observationTypes;

    private final int scoreDefinitions;


    /*
     * ================================================================
     * CONSTRUCTOR
     * ================================================================
     */

    public MasterDataSyncResult(
            boolean success,
            String message
    ) {

        this(
                success,
                message,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );
    }


    /*
     * ================================================================
     * FULL CONSTRUCTOR
     * ================================================================
     */

    public MasterDataSyncResult(
            boolean success,
            String message,
            int farms,
            int sections,
            int greenhouses,
            int bays,
            int beds,
            int scanningPoints,
            int scanningPointBeds,
            int fcmTraps,
            int insectTypes,
            int observationTypes,
            int scoreDefinitions
    ) {

        this.success =
                success;

        this.message =
                message;

        this.farms =
                farms;

        this.sections =
                sections;

        this.greenhouses =
                greenhouses;

        this.bays =
                bays;

        this.beds =
                beds;

        this.scanningPoints =
                scanningPoints;

        this.scanningPointBeds =
                scanningPointBeds;

        this.fcmTraps =
                fcmTraps;

        this.insectTypes =
                insectTypes;

        this.observationTypes =
                observationTypes;

        this.scoreDefinitions =
                scoreDefinitions;
    }


    /*
     * ================================================================
     * GETTERS
     * ================================================================
     */

    public boolean isSuccess() {

        return success;
    }


    public String getMessage() {

        return message;
    }


    public int getFarms() {

        return farms;
    }


    public int getSections() {

        return sections;
    }


    public int getGreenhouses() {

        return greenhouses;
    }


    public int getBays() {

        return bays;
    }


    public int getBeds() {

        return beds;
    }


    public int getScanningPoints() {

        return scanningPoints;
    }


    public int getScanningPointBeds() {

        return scanningPointBeds;
    }


    public int getFcmTraps() {

        return fcmTraps;
    }


    public int getInsectTypes() {

        return insectTypes;
    }


    public int getObservationTypes() {

        return observationTypes;
    }


    public int getScoreDefinitions() {

        return scoreDefinitions;
    }
}