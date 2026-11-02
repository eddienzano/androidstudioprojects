package com.unitedselections.intake;

public class ScanItem {
    public int intakeId;
    public String variety;
    public int maxQty;
    public int enteredQty = 0;

    public ScanItem(int id, String v, int q){
        intakeId = id;
        variety = v;
        maxQty = q;
    }
}