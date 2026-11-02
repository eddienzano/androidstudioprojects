package com.unitedselections.admin;

import java.util.List;

public class ScanoutGroup {

    public String destination;
    public String targetName;
    public String outDate;
    public String harvestDate;
    public List<ScanoutItem> items;

    public ScanoutGroup(String destination, String targetName,
                        String outDate, String harvestDate,
                        List<ScanoutItem> items) {

        this.destination = destination;
        this.targetName = targetName;
        this.outDate = outDate;
        this.harvestDate = harvestDate;
        this.items = items;
    }
}
