package com.hillary.customer;

public class ProductModel {

    private int id;
    private String product_name;
    private String description;
    private String category;
    private String unit;
    private double price;
    private int stock;
    private String thumbnail;
    private String supplier_name;

    public ProductModel(int id,
                        String product_name,
                        String description,
                        String category,
                        String unit,
                        double price,
                        int stock,
                        String thumbnail,
                        String supplier_name) {

        this.id = id;
        this.product_name = product_name;
        this.description = description;
        this.category = category;
        this.unit = unit;
        this.price = price;
        this.stock = stock;
        this.thumbnail = thumbnail;
        this.supplier_name = supplier_name;
    }

    public int getId() {
        return id;
    }

    public String getProduct_name() {
        return product_name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getUnit() {
        return unit;
    }

    public double getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getSupplier_name() {
        return supplier_name;
    }
}