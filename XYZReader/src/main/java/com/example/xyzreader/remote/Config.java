package com.example.xyzreader.remote;

import android.support.v4.text.TextUtilsCompat;
import android.support.v4.view.ViewCompat;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

public class Config {
    public static final URL BASE_URL;

    static {
        URL url = null;
        try {
            url = new URL("https://dl.dropboxusercontent.com/u/231329/xyzreader_data/data.json" );
        } catch (MalformedURLException ignored) {
            // TODO: throw a real error
        }

        BASE_URL = url;
    }


    public static boolean isRTL() {
       return TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }


}
