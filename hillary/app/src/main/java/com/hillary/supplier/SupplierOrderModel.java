package com.hillary.supplier;

public class SupplierOrderModel {

    int id;

    String customerName;

    String status;

    String createdAt;

    double totalAmount;

    public SupplierOrderModel(
            int id,
            String customerName,
            String status,
            String createdAt,
            double totalAmount
    ) {

        this.id = id;
        this.customerName = customerName;
        this.status = status;
        this.createdAt = createdAt;
        this.totalAmount = totalAmount;
    }

    public int getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
}