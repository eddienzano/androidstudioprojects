package com.hillary.products.inventory;

public class ProductInventory {

    private String id;
    private String product_name;
    private String supplier_name;
    private String thumbnail;
    private String stock;
    private String category;
    private String sku;
    private String price;

    public ProductInventory(
            String id,
            String product_name,
            String supplier_name,
            String thumbnail,
            String stock,
            String category,
            String sku,
            String price) {

        this.id = id;
        this.product_name = product_name;
        this.supplier_name = supplier_name;
        this.thumbnail = thumbnail;
        this.stock = stock;
        this.category = category;
        this.sku = sku;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public String getProduct_name() {
        return product_name;
    }

    public String getSupplier_name() {
        return supplier_name;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getStock() {
        return stock;
    }

    public String getCategory() {
        return category;
    }

    public String getSku() {
        return sku;
    }

    public String getPrice() {
        return price;
    }

    public double getInventoryValue() {

        try {

            double stockValue =
                    Double.parseDouble(stock);

            double priceValue =
                    Double.parseDouble(price);

            return stockValue * priceValue;

        } catch (Exception e){

            return 0;
        }
    }
}