package com.hillary.customer;

public class OrderModel {

    private int id;

    private String orderNumber;

    private double totalAmount;

    private String status;

    private String createdAt;

    public OrderModel(
            int id,
            String orderNumber,
            double totalAmount,
            String status,
            String createdAt
    ) {

        this.id = id;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}