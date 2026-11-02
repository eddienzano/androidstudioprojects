package com.hillary.network;

import android.content.Context;

import com.hillary.utils.SessionManager;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class ApiClient {

    private static OkHttpClient client;

    public static OkHttpClient getClient(Context context) {

        if (client == null) {

            SessionManager session = new SessionManager(context);

            Interceptor authInterceptor = chain -> {

                Request original = chain.request();

                Request.Builder builder = original.newBuilder()
                        .addHeader("Accept", "application/json");

                String token = session.getToken();

                if (token != null && !token.isEmpty()) {
                    builder.addHeader("Authorization", "Bearer " + token);
                }

                return chain.proceed(builder.build());
            };

            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(authInterceptor)
                    .build();
        }

        return client;
    }

    public static OkHttpClient getPublicClient() {

        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}