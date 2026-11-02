package com.hillary.products.inventory;

public class InventoryLog {

    private String productName;
    private String thumbnail;
    private String transactionType;
    private String quantity;
    private String previousStock;
    private String newStock;
    private String notes;
    private String createdAt;

    public InventoryLog(String productName,
                        String thumbnail,
                        String transactionType,
                        String quantity,
                        String previousStock,
                        String newStock,
                        String notes,
                        String createdAt) {

        this.productName = productName;
        this.thumbnail = thumbnail;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public String getProductName() {
        return productName;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getPreviousStock() {
        return previousStock;
    }

    public String getNewStock() {
        return newStock;
    }

    public String getNotes() {
        return notes;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}