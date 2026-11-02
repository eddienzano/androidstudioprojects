package com.aaagrowers.scoutingapp.data.sync;

public class SyncResult {

    public boolean success;

    public int roundsSynced;

    public int observationsSynced;

    public int severePointsSynced;

    public int gpsTracksSynced;

    public int failed;

    public String error;

    public SyncResult() {
        success = true;
    }
}