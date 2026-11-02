package com.hillary.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "hillary_app";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    /*TRANSPORTER */
    private static final String TRANSPORTER_ID = "transporter_id";
    private static final String TRANSPORTER_NAME = "transporter_name";
    private static final String TRANSPORTER_PHONE = "transporter_phone";
    private static final String ACCOUNT_TYPE = "account_type";

    /* SUPPLIER */

    private static final String TOKEN = "token";
    private static final String USER_ID = "user_id";
    private static final String NAME = "name";
    private static final String EMAIL = "email";

    /* CUSTOMER */

    private static final String CUSTOMER_ID = "customer_id";
    private static final String CUSTOMER_NAME = "customer_name";
    private static final String CUSTOMER_EMAIL = "customer_email";

    /* GENERAL */



    public SessionManager(Context context){

        prefs = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );

        editor = prefs.edit();
    }

    /* =====================================================
       SUPPLIER SESSION
    ===================================================== */

    public void saveSupplierSession(
            String token,
            String name,
            int userId,
            String email
    ){

        editor.putString(TOKEN, token);

        editor.putString(NAME, name);

        editor.putInt(USER_ID, userId);

        editor.putString(EMAIL, email);

        editor.putString(ACCOUNT_TYPE, "supplier");

        editor.apply();
    }

    public String getToken(){

        return prefs.getString(TOKEN, null);
    }

    public int getUserId(){

        return prefs.getInt(USER_ID, 0);
    }

    public String getName(){

        return prefs.getString(NAME, "");
    }

    public String getEmail(){

        return prefs.getString(EMAIL, "");
    }

    public boolean isSupplierLoggedIn(){

        return getToken() != null
                &&
                "supplier".equals(
                        prefs.getString(ACCOUNT_TYPE, "")
                );
    }

    /* =====================================================
       CUSTOMER SESSION
    ===================================================== */

    public void saveCustomerSession(
            int customerId,
            String customerName,
            String customerEmail
    ){

        editor.putInt(CUSTOMER_ID, customerId);

        editor.putString(CUSTOMER_NAME, customerName);

        editor.putString(CUSTOMER_EMAIL, customerEmail);

        editor.putString(ACCOUNT_TYPE, "customer");

        editor.apply();
    }

    public int getCustomerId(){

        return prefs.getInt(CUSTOMER_ID, 0);
    }

    public String getCustomerName(){

        return prefs.getString(CUSTOMER_NAME, "");
    }

    public String getCustomerEmail(){

        return prefs.getString(CUSTOMER_EMAIL, "");
    }

    public boolean isCustomerLoggedIn(){

        return prefs.contains(CUSTOMER_ID)
                &&
                "customer".equals(
                        prefs.getString(ACCOUNT_TYPE, "")
                );
    }

    /* =====================================================
       GENERAL
    ===================================================== */

    public String getAccountType(){

        return prefs.getString(ACCOUNT_TYPE, "");
    }

    public boolean isLoggedIn(){

        return isSupplierLoggedIn()
                ||
                isCustomerLoggedIn();
    }

    public void logout(){

        editor.clear();

        editor.apply();
    }

    public void saveTransporter(String id, String name, String phone) {

        editor.putString(TRANSPORTER_ID, id);
        editor.putString(TRANSPORTER_NAME, name);
        editor.putString(TRANSPORTER_PHONE, phone);
        editor.putString(ACCOUNT_TYPE, "transporter");
        editor.apply();
    }

    public boolean isTransporterLoggedIn() {
        return prefs.contains(TRANSPORTER_ID)
                && "transporter".equals(prefs.getString(ACCOUNT_TYPE, ""));
    }

    public String getTransporterId() {
        return prefs.getString(TRANSPORTER_ID, null);
    }


}