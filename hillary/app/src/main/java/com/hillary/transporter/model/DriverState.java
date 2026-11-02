package com.hillary.transporter.model;

public enum DriverState {

    OFFLINE,
    ONLINE,
    IDLE,

    JOB_ASSIGNED,

    ENROUTE_PICKUP,
    AT_PICKUP,

    OUT_FOR_DELIVERY,

    DELIVERED,

    COMPLETED
}