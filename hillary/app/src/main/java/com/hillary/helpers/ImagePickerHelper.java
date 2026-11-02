package com.hillary.helpers;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public class ImagePickerHelper {

    public static final int PICK_IMAGE_REQUEST = 1001;

    public static void pickImage(Activity activity) {

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");

        activity.startActivityForResult(
                intent,
                PICK_IMAGE_REQUEST
        );
    }

    public static Uri getImageResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == Activity.RESULT_OK
                && data != null) {

            return data.getData();
        }

        return null;
    }
}