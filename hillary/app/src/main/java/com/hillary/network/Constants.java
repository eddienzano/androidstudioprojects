package com.hillary.network;

public class Constants {

    private static final String BASE =
            "https://www.supplybridge.co.ke/fillers/api/";

    /* AUTH */
    public static final String REGISTER_URL = BASE + "auth/register.php";
    public static final String LOGIN_URL    = BASE + "auth/login.php";
    public static final String ME_URL       = BASE + "auth/me.php";
    public static final String LOGOUT_URL   = BASE + "auth/logout.php";
    public static final String VERIFY_OTP_URL = BASE + "auth/verify_otp.php";
    public static final String FORGOT_PASSWORD_URL = BASE + "auth/forgot_password.php";

    public static final String MY_PRODUCTS_URL = BASE + "products/my_products.php";

    public static final String CREATE_PRODUCT_URL =
            BASE + "products/create.php";

    public static final String SUPPLIER_ME_URL =
            BASE + "suppliers/me.php";

    public static final String UPLOAD_PRODUCT_IMAGE_URL =
            BASE + "products/upload_image.php";


    public static final String UPDATE_SUPPLIER_URL =
            BASE + "suppliers/update.php";

    public static final String GET_PRODUCT_URL =
            BASE + "products/get_product.php";

    public static final String UPDATE_PRODUCT_URL =
            BASE + "products/update_product.php";


    public static final String DELETE_PRODUCT_URL =
            BASE + "products/delete_product.php";

    public static final String MARKETPLACE_PRODUCTS_URL =
            BASE + "customer/get_marketplace_products.php";


    public static final String PRODUCT_DETAILS_URL =
            BASE + "customer/get_product_details.php";

    public static final String ADD_TO_CART_URL =
            BASE + "customer/add_to_cart.php";

    public static final String CUSTOMER_LOGIN_URL =
            BASE + "customer/login.php";

    public static final String CUSTOMER_REGISTER_URL =
            BASE + "customer/customer_register.php";


/* =========================
   CART
========================= */


    public static final String GET_CART_URL =
            BASE + "customer/get_cart.php";

    public static final String UPDATE_CART_QTY_URL =
            BASE + "customer/update_cart_qty.php";

    public static final String REMOVE_CART_ITEM_URL =
            BASE + "customer/remove_cart_item.php";

    public static final String PLACE_ORDER_URL =
            BASE + "customer/place_order.php";


    public static final String GET_CUSTOMER_ORDERS_URL =
            BASE + "customer/get_customer_orders.php";

    public static final String GET_ORDER_DETAILS_URL =
            BASE + "customer/get_order_details.php";

    public static final String GET_SUPPLIER_ORDERS_URL =
            BASE + "suppliers/get_supplier_orders.php";

    public static final String UPDATE_ORDER_STATUS_URL =
            BASE + "suppliers/update_order_status.php";


    public static final String TRANSPORTER_LOGIN_URL =
            BASE + "transporter/login.php";

    public static final String TRANSPORTER_DASHBOARD_URL =
            BASE + "transporter/dashboard.php";

    public static final String MY_DELIVERIES_URL =
            BASE + "transporter/my_deliveries.php";

    public static final String UPDATE_DRIVER_LOCATION_URL =
            BASE + "transporter/update_location.php";

    public static final String UPDATE_DELIVERY_STATUS_URL =
            BASE + "transporter/update_location.php";

    public static final String GOOGLE_MAPS_API_KEY =
            "AIzaSyAVBR66NnefbbKhYa0mzzMzR0xUcAOPYfk";

    public static final String GOOGLE_DIRECTIONS_URL =
            "https://maps.googleapis.com/maps/api/directions/json";

    public static final String START_TRIP_URL =
            BASE + "transporter/start_trip.php";
}



