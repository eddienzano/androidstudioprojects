package com.aaagrowers.scoutingapp.data.sync;

public final class SyncStatus {

    private SyncStatus() {
    }

    public static final String PENDING = "pending";

    public static final String SYNCING = "syncing";

    public static final String SYNCED = "synced";

    public static final String FAILED = "failed";
}