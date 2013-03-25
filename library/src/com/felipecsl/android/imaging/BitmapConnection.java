package com.felipecsl.android.imaging;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

class BitmapConnection {
    public interface Runnable<T> {
        T run(InputStream stream);
    }

    public <T> T readStream(final String urlString, final Runnable<T> runnable) {
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setUseCaches(true);
            inputStream = new PatchInputStream(urlConnection.getInputStream());
            return runnable.run(inputStream);
        } catch (final IOException e) {
            Log.e("BitmapConnection", "", e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (final IOException e) {}
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
