package com.constant.quest;

/**
 * Created by tony on 12/6/13.
 */

import java.util.Locale;


public class Constants {
    public static final String ACCESS_KEY_ID = "AKIAIRIR3GB3JSY5GMMA";
    public static final String SECRET_KEY = "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";

    public static final String PICTURE_BUCKET = "picture-bucket";



    public static String getPictureBucket() {
        return ("my-unique-name" + ACCESS_KEY_ID + PICTURE_BUCKET).toLowerCase(Locale.US);
    }
}
