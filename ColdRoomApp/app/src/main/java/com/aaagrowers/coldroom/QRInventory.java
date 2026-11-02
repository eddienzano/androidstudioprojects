package com.aaagrowers.coldroom;

public class QRInventory {
    public String qrCode;
    public String varietyName;
    public String intakeTime;
    public String farm;
    public int quantity; // new field

    // Updated constructor
    public QRInventory(String qrCode, String varietyName, String intakeTime, String farm, int quantity) {
        this.qrCode = qrCode;
        this.varietyName = varietyName;
        this.intakeTime = intakeTime;
        this.farm = farm;
        this.quantity = quantity;
    }
}