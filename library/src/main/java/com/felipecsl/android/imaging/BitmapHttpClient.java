package com.felipecsl.android.imaging;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.commons.io.IOUtils;

import com.squareup.okhttp.OkHttpClient;

public class BitmapHttpClient {
    private static final String TAG = "BitmapHttpClient";
    private static OkHttpClient client = new OkHttpClient();

    public static byte[] get(final String urlString) {
        InputStream in = null;
        try {
            final String decodedUrl = URLDecoder.decode(urlString, "UTF-8");
            final URL url = new URL(decodedUrl);
            final HttpURLConnection connection = client.open(url);
            in = connection.getInputStream();
            return IOUtils.toByteArray(in);
        } catch (final MalformedURLException e) {
            Log.d(TAG, "Malformed URL", e);
        } catch (final OutOfMemoryError e) {
            Log.d(TAG, "Out of memory", e);
        } catch (final UnsupportedEncodingException e) {
            Log.d(TAG, "Unsupported encoding", e);
        } catch (final IOException e) {
            Log.d(TAG, "IO exception", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException e) {}
            }
        }
        return null;
    }
}