package com.hillary.network;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.*;

public class MultipartUploader {

    private static final String TAG = "IMAGE_UPLOAD";

    public static void uploadProductImage(
            Context context,
            String token,
            String productId,
            Uri imageUri,
            Callback callback
    ) {

        try {

            Log.d(TAG, "Preparing image upload for product: " + productId);

            File file = createTempFile(context, imageUri);

            Log.d(TAG, "Temp file created: " + file.getAbsolutePath());

            RequestBody fileBody = RequestBody.create(
                    file,
                    MediaType.parse("image/*")
            );

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("product_id", productId)
                    .addFormDataPart("image", file.getName(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(Constants.UPLOAD_PRODUCT_IMAGE_URL)
                    .addHeader("Authorization", "Bearer " + token)
                    .post(requestBody)
                    .build();

            Log.d(TAG, "Sending image upload request...");

            OkHttpClient client = ApiClient.getClient(context);
            client.newCall(request).enqueue(callback);

        } catch (Exception e) {
            Log.e(TAG, "UPLOAD EXCEPTION", e);
        }
    }

    private static File createTempFile(Context context, Uri uri) throws IOException {

        InputStream inputStream = context.getContentResolver().openInputStream(uri);

        File file = File.createTempFile("upload_", ".jpg", context.getCacheDir());

        FileOutputStream outputStream = new FileOutputStream(file);

        byte[] buffer = new byte[4096];
        int read;

        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }

        outputStream.close();
        inputStream.close();

        return file;
    }
}