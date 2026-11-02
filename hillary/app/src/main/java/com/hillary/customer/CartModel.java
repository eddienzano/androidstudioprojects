package com.hillary.customer;

public class CartModel {

    private int cartId;
    private int productId;

    private String productName;
    private String thumbnail;

    private double price;

    private int qty;
    private int stock;

    public CartModel(
            int cartId,
            int productId,
            String productName,
            String thumbnail,
            double price,
            int qty,
            int stock
    ) {

        this.cartId = cartId;
        this.productId = productId;
        this.productName = productName;
        this.thumbnail = thumbnail;
        this.price = price;
        this.qty = qty;
        this.stock = stock;
    }

    public int getCartId() {
        return cartId;
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

    public int getStock() {
        return stock;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }
}