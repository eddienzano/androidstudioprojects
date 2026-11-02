package com.hillary.customer;

public class OrderItemModel {

    private int productId;

    private String productName;

    private String thumbnail;

    private double price;

    private int qty;

    private double subtotal;

    public OrderItemModel(
            int productId,
            String productName,
            String thumbnail,
            double price,
            int qty,
            double subtotal
    ) {

        this.productId = productId;
        this.productName = productName;
        this.thumbnail = thumbnail;
        this.price = price;
        this.qty = qty;
        this.subtotal = subtotal;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public double getPrice() {
        return price;
    }

    public int getQty() {
        return qty;
    }

    public double getSubtotal() {
        return subtotal;
    }
}