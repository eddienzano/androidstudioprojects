package com.hillary.supplier;

public class SupplierOrderItemModel {

    String productName;
    int qty;
    double price;
    double subtotal;

    public SupplierOrderItemModel(String productName, int qty, double price, double subtotal) {
        this.productName = productName;
        this.qty = qty;
        this.price = price;
        this.subtotal = subtotal;
    }

    public String getProductName() { return productName; }
    public int getQty() { return qty; }
    public double getPrice() { return price; }
    public double getSubtotal() { return subtotal; }
}