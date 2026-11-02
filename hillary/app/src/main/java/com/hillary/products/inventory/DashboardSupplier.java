package com.hillary.products.inventory;

public class DashboardSupplier {

    String supplier_name;
    String total_products;
    String total_stock;

    public DashboardSupplier(String supplier_name,
                             String total_products,
                             String total_stock) {

        this.supplier_name = supplier_name;
        this.total_products = total_products;
        this.total_stock = total_stock;
    }

    public String getSupplier_name() {
        return supplier_name;
    }

    public String getTotal_products() {
        return total_products;
    }

    public String getTotal_stock() {
        return total_stock;
    }
}