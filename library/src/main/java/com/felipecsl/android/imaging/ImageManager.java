package com.felipecsl.android.imaging;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import com.felipecsl.android.Utils;
import com.felipecsl.android.imaging.CacheManager.CacheManagerCallback;

public class ImageManager {
    /* Static members */
    private static final String TAG = "ImageManager";
    public static final int NO_PLACEHOLDER = -1;

    // TODO: Should be removed once a job is finished
    private static final Map<ImageView, String> runningJobs = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    private static CacheManager defaultCacheManager;
    private static final Handler uiHandler = new Handler(Looper.getMainLooper());

    /* Instance members */
    private final Context context;
    private final CacheManager cacheManager;
    private ImageViewCallback imageViewCallback;
    private BitmapCallback bitmapCallback;
    private int placeholderResId = Color.parseColor("#eeeeee");
    public static final boolean LOG_CACHE_OPERATIONS = false;

    public interface ImageViewCallback {
        void onImageLoaded(ImageView imageView, Bitmap bitmap);
    }

    public interface BitmapCallback {
        void onBitmapLoaded(Bitmap bitmap);
    }

    /**
     * Default constructor.
     * <p>
     * Will use the default CacheManager.
     *
     */
    public ImageManager(final Context context) {
        this(context,
                defaultCacheManager == null
                        ? (defaultCacheManager = new CacheManager(
                        Utils.createDefaultBitmapLruCache(),
                        DiskLruImageCache.getInstance(context)))
                        : defaultCacheManager);
    }

    /**
     * Designated constructor
     * <p>
     * Provide your own custom LruCache if you wish to.
     * <p>
     *
     * @param context
     * @param _cacheManager custom cache manager to be used
     */
    public ImageManager(final Context context, final CacheManager _cacheManager) {

        this.context = context;
        cacheManager = _cacheManager;
    }

    public Context getContext() {
        return context;
    }

    public static void cleanUp() {
        defaultCacheManager.clear();
        runningJobs.clear();
    }

    /**
     * Loads an image from the provided URL and caches it in the manager's LRU and Disk cache.
     * If the image is already cached, fetches it from the cache instead.
     * <p>
     * Upon completion, assigns the loaded bitmap into the provided imageView object and calls the
     * imageViewCallback with the loaded ImageView object.
     *
     * @param urlString the URL string to load the image from
     * @param imageView the ImageView that will receive the loaded image
     * @param options Load options
     */
    public void loadImage(final String urlString, final ImageView imageView, final JobOptions options) {
        if (urlString == null || urlString == "")
            return;

        final ImageManager self = this;

        loadImage(urlString, imageView, options, new CacheManagerCallback() {
            @Override
            public void onBitmapLoaded(final Bitmap bitmap, final LoadedFrom source) {
                if (bitmap == null) {
                    queueJob(urlString, imageView, options);
                } else {
                    final ProcessorCallback callback = new ProcessorCallback(self, urlString, imageView, options);
                    callback.onBitmapLoaded(bitmap, source);
                }
            }
        });
    }

    public void loadImage(final Uri imageUri, final ImageView imageView, final JobOptions options) {
        if (imageUri == null)
            return;

        final ImageManager self = this;
        final String urlString = imageUri.toString();

        loadImage(urlString, imageView, options, new CacheManagerCallback() {
            @Override
            public void onBitmapLoaded(final Bitmap bitmap, final LoadedFrom source) {
                if (bitmap == null) {
                    queueJob(imageUri, imageView, options);
                } else {
                    final ProcessorCallback callback = new ProcessorCallback(self, urlString, imageView, options);
                    callback.onBitmapLoaded(bitmap, source);
                }
            }
        });
    }

    private void loadImage(final String urlString, final ImageView imageView, final JobOptions options, final CacheManagerCallback callback) {
        runningJobs.put(imageView, urlString);

        if (placeholderResId != NO_PLACEHOLDER)
            CacheableDrawable.setPlaceholder(imageView, placeholderResId, null);

        cacheManager.get(getCacheKeyForJob(urlString, options), callback);
    }

    public static String getCacheKeyForJob(final String url, final JobOptions options) {
        if (options.requestedHeight <= 0 && options.requestedWidth <= 0)
            return url;
        return String.format("%s-%sx%s", url, options.requestedWidth, options.requestedHeight);
    }

    /**
     * Loads an image from the provided URL and caches it in the manager's LRU and Disk cache.
     * If the image is already cached, fetches it from the cache instead.
     * Calls the BitmapCallback upon completion, with the loaded Bitmap instance.
     *
     * @param urlString the URL string to load the image from
     */
    public void loadImage(final String urlString) {
        if (urlString == null || urlString == "")
            return;

        cacheManager.get(urlString, new CacheManagerCallback() {
            @Override
            public void onBitmapLoaded(final Bitmap bitmap, final LoadedFrom source) {
                if (bitmap != null) {
                    if (bitmapCallback != null) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                bitmapCallback.onBitmapLoaded(bitmap);
                            }
                        });
                    }
                } else {
                    queueJob(urlString);
                }
            }
        });
    }

    public int getPlaceholderResId() {
        return placeholderResId;
    }

    public void setPlaceholderResId(final int placeholderResId) {
        this.placeholderResId = placeholderResId;
    }

    /**
     * Private
     *
     * @hide
     */

    private void queueJob(final String url) {
        final byte[] binaryData = BitmapHttpClient.get(url);

        if (binaryData == null) {
            Log.e(TAG, "queueJob got null binaryData");
            return;
        }

        final Bitmap bitmap = BitmapProcessor.decodeByteArray(binaryData, null);

        if (bitmap == null) {
            Log.e(TAG, "queueJob got NULL bitmap");
            return;
        }

        cacheManager.put(url, bitmap);

        if (bitmapCallback != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    bitmapCallback.onBitmapLoaded(bitmap);
                }
            });
        }

        if (LOG_CACHE_OPERATIONS) {
            Log.d(TAG, "Image downloaded: " + url);
        }
    }

    private void queueJob(final String url, final ImageView imageView, final JobOptions options) {
        final ProcessorCallback callback = new ProcessorCallback(this, url, imageView, options);

        BitmapProcessor.decodeSampledBitmapFromRemoteUrl(context, url, options.requestedWidth, options.requestedHeight, callback);
    }

    private void queueJob(final Uri uri, final ImageView imageView, final JobOptions options) {
        final ProcessorCallback callback = new ProcessorCallback(this, uri.toString(), imageView, options);

        BitmapProcessor.decodeSampledBitmapFromLocalUri(context, uri, options.requestedWidth, options.requestedHeight, callback);
    }

    public void setImageViewCallback(final ImageViewCallback callback) {
        imageViewCallback = callback;
    }

    public void setBitmapCallback(final BitmapCallback bitmapCallback) {
        this.bitmapCallback = bitmapCallback;
    }

    public ImageViewCallback getImageViewCallback() {
        return imageViewCallback;
    }

    public BitmapCallback getBitmapCallback() {
        return bitmapCallback;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public Map<ImageView, String> getRunningJobs() {
        return runningJobs;
    }
}
