package com.aaagrowers.coldroom.storage;

public class BoxFillItem {

    public long boxId;
    public String qrRaw;

    // ✅ NEW (PRO level)
    public int length;
    public int stems;

    public int synced; // 👈 ADD THIS

    public BoxFillItem(long boxId, String qrRaw, int length, int stems) {
        this.boxId = boxId;
        this.qrRaw = qrRaw;
        this.length = length;
        this.stems = stems;
    }
}