package com.felipecsl.android.imaging.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.GridView;
import android.widget.Toast;

import com.felipecsl.android.imaging.DiskLruImageCache;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;

public class MainActivity extends Activity {

    private final AsyncHttpClient httpClient;
    private static final String urlTemplate = "http://farm%s.staticflickr.com/%s/%s_%s.jpg";
    private static final String apiUrl = "http://api.flickr.com/services/rest";
    private ListAdapter adapter;
    private GridView gridView;

    public MainActivity() {
        httpClient = new AsyncHttpClient();
        httpClient.getHttpClient().getParams().setParameter("http.protocol.single-cookie-header", true);
        httpClient.getHttpClient().getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        gridView = (GridView)findViewById(R.id.gridView);

        final PersistentCookieStore myCookieStore = new PersistentCookieStore(this);
        httpClient.setCookieStore(myCookieStore);

        if (!DiskLruImageCache.isInitialized()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(final Void... params) {
                    DiskLruImageCache.getInstance(MainActivity.this);
                    return null;
                }

                @Override
                protected void onPostExecute(final Void result) {
                    initGrid();
                }
            }.execute();
        }
    }

    protected void initGrid() {
        final List<String> imageUrls = new ArrayList<String>();

        final Map<String, String> queryString = new HashMap<String, String>();
        queryString.put("method", "flickr.photos.search");
        queryString.put("api_key", "6f906ffb12c888dde723dd6eba2a9d9f");
        queryString.put("text", "cat");
        queryString.put("format", "json");
        queryString.put("nojsoncallback", "1");
        final RequestParams requestParams = new RequestParams(queryString);
        final JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(final JSONObject json) {
                try {
                    final JSONArray photos = json.getJSONObject("photos").getJSONArray("photo");
                    final int length = photos.length();

                    for (int i = 0; i < length; i++) {
                        final JSONObject photoObj = photos.getJSONObject(i);

                        imageUrls.add(String.format(
                            urlTemplate,
                            String.valueOf(photoObj.getInt("farm")),
                            photoObj.getString("server"),
                            photoObj.getString("id"),
                            photoObj.getString("secret")));
                    }

                    adapter = new ListAdapter(MainActivity.this, imageUrls);

                    gridView.setAdapter(adapter);
                } catch (final JSONException e) {
                    Log.e("MainActivity", "JSON Exception parsing Flickr API", e);
                    Toast.makeText(MainActivity.this, "Sorry there was an exception parsing Flickr API", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(final Throwable arg0, final String arg1) {
                Log.e("MainActivity", "onFailure " + arg1);
                Toast.makeText(MainActivity.this, "Sorry there was an error parsing Flickr API", Toast.LENGTH_LONG).show();
            }
        };
        httpClient.get(apiUrl, requestParams, responseHandler);
    }
}
