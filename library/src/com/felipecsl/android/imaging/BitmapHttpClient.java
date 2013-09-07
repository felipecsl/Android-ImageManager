package com.felipecsl.android.imaging;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class BitmapHttpClient {
    private static final String TAG = "BitmapHttpClient";
    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(final String urlString, final RequestParams params, final BinaryHttpResponseHandler responseHandler) {
        try {
            final String decodedUrl = URLDecoder.decode(urlString, "UTF-8");
            final URL url = new URL(decodedUrl);
            final URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            client.get(uri.toASCIIString(), params, responseHandler);
        } catch (final MalformedURLException e) {
            Log.e(TAG, "", e);
        } catch (final URISyntaxException e) {
            Log.e(TAG, "", e);
        } catch (final UnsupportedEncodingException e) {
            Log.e(TAG, "", e);
        } catch (final IllegalArgumentException e) {
            Log.e(TAG, "", e);
        }
    }
}
