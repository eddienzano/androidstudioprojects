package com.aaagrowers.coldroom.summers;

public class SummersScanItem {

    public String qr;
    public String status;
    public String time;

    public SummersScanItem(
            String qr,
            String status,
            String time
    ){
        this.qr = qr;
        this.status = status;
        this.time = time;
    }
}