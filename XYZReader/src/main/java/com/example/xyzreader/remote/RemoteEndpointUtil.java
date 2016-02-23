package com.example.xyzreader.remote;

import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class RemoteEndpointUtil {
    private static final String TAG = "RemoteEndpointUtil";

    private RemoteEndpointUtil() {
    }

    public static JSONArray fetchJsonArray() {
        String itemsJson = null;
        try {
            itemsJson = fetchPlainText(Config.BASE_URL);
        } catch (IOException e) {
            Log.e(TAG, "Error fetching items JSON", e);
            return null;
        }

        // Parse JSON
        try {
            JSONTokener tokener = new JSONTokener(itemsJson);
            Object val = tokener.nextValue();
            if (!(val instanceof JSONArray)) {
                throw new JSONException("Expected JSONArray");
            }
            return (JSONArray) val;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing items JSON", e);
        }

        return null;
    }

    static String fetchPlainText(URL url) throws IOException {
        return new String(fetch(url), "UTF-8" );
    }

    static byte[] fetch(URL url) throws IOException {
        InputStream in = null;

        try {
            OkHttpClient client = new OkHttpClient();

            // BUG: com.squareup.okhttp:okhttp:1.1.0 version generate
            // "Fatal signal 11 (sigsegv) at 0x00000000 (code=1)" errors, and app closes abruptly
            // On old Android API 16, 17, 18 versions.
            // This has been very hard to detect, using com.squareup.okhttp:okhttp:2.2.0 solves
            // the problem!!
            HttpURLConnection conn = new OkUrlFactory(client).open(url);
            //HttpURLConnection conn = client.open(url);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            return out.toByteArray();

        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
