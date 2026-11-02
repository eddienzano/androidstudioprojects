package com.yourapp;

public class VarietyEntry {
    public int greenhouseId;
    public String greenhouseName;
    public String displayName;
    public int varietyId;
    public String varietyName;
    public int farmId;

    public VarietyEntry(int ghId, String ghName, String disp, int vId, String vName, int fId) {
        this.greenhouseId = ghId;
        this.greenhouseName = ghName;
        this.displayName = disp;
        this.varietyId = vId;
        this.varietyName = vName;
        this.farmId = fId;
    }
}