package com.felipecsl.android.imaging;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import com.felipecsl.android.BuildConfig;
import com.felipecsl.android.Utils;
import com.felipecsl.android.imaging.BitmapProcessor.BitmapProcessorCallback;
import com.loopj.android.http.BinaryHttpResponseHandler;

public class ImageManager {
    private static final String TAG = "ImageManager";

    /* Static members */
    public static final int NO_PLACEHOLDER = -1;

    public static final boolean LOG_CACHE_OPERATIONS = false;

    /* Static members */
    private static final Map<ImageView, String> runningJobs = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    private static CacheManager cacheManager;
    private static final boolean DEBUG = false;

    /* Instance members */
    private final Context context;

    /**
     * A reference to the LruCache this instance will use.
     */
    private final LruCache<String, Bitmap> designatedMemoryCache;

    /**
     * Current size of the global cache
     */
    private static float globalCacheCurrentSize;

    /**
     * Time in milliseconds before cache had to purge for the first time
     */
    private static long timeBeforeFirstCachePurge;

    private ImageViewCallback imageViewCallback;
    private BitmapCallback bitmapCallback;
    private Drawable placeholderDrawable;

    public interface ImageViewCallback {
        void onImageLoaded(ImageView imageView, Bitmap bitmap);
    }

    public interface BitmapCallback {
        void onBitmapLoaded(Bitmap bitmap);
    }

    public static class JobOptions {
        public boolean roundedCorners = false;
        public boolean fadeIn = false;
        public boolean centerCrop = false;
        public int cornerRadius = 5;
        public int requestedWidth;
        public int requestedHeight;

        public JobOptions() {
            this(0, 0);
        }

        public JobOptions(final ImageView imgView) {
            this(imgView.getWidth(), imgView.getHeight());
        }

        public JobOptions(final int requestedWidth, final int requestedHeight) {
            this.requestedWidth = requestedWidth;
            this.requestedHeight = requestedHeight;
        }
    }

    /**
     * Default constructor.
     * <p>
     * Will use the global LruCache.
     * 
     * @param context
     */
    public ImageManager(final Context context) {
        this(context, null);
    }

    /**
     * Designated constructor
     * <p>
     * Provide your own custom LruCache if you wish to.
     * <p>
     * A null LruCache will indicate the desire to use the global LruCache.
     * <p>
     * A custom LruCache will not cache to the global DiskLruCache for optimization purposes.
     * 
     * @param context
     * @param customLruCache can be null
     */
    public ImageManager(final Context context, final LruCache<String, Bitmap> customLruCache) {
        this.context = context;

        // One-time initialization of ImageManager
        if (cacheManager == null) {
            cacheManager = new CacheManager(context, Utils.createDefaultBitmapLruCache(), DiskLruImageCache.getInstance(context));
            timeBeforeFirstCachePurge = System.currentTimeMillis();
        }

        designatedMemoryCache = customLruCache != null ? customLruCache : cacheManager.getMemoryCache();

        // Default placeholder
        placeholderDrawable = new ColorDrawable(0xeeeeee);
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
        if (urlString == null || urlString == "") {
            return;
        }

        CacheableDrawable.setPlaceholder(imageView, context, 0, placeholderDrawable, BuildConfig.DEBUG);

        runningJobs.put(imageView, urlString);

        Bitmap bitmap = getBitmapFromLRUCache(urlString);

        if (bitmap != null) {
            setImageDrawable(imageView, bitmap, options, LoadedFrom.MEMORY);
            return;
        }

        bitmap = getBitmapFromDiskCache(urlString);

        if (bitmap != null) {
            setImageDrawable(imageView, bitmap, options, LoadedFrom.DISK);
            return;
        }

        queueJob(urlString, imageView, options);
    }

    public Drawable getPlaceholderResId() {
        return placeholderDrawable;
    }

    public void setPlaceholderDrawable(final Drawable placeholderDrawable) {
        this.placeholderDrawable = placeholderDrawable;
    }

    public boolean isUsingGlobalCache() {
        return designatedMemoryCache.equals(cacheManager.getMemoryCache());
    }

    /**
     * Private
     * 
     * @hide
     */

    private Bitmap getBitmapFromCache(final String urlString) {
        final Bitmap bitmap = getBitmapFromLRUCache(urlString);

        if (bitmap != null)
            return bitmap;

        return getBitmapFromDiskCache(urlString);
    }

    @SuppressWarnings("unused")
    private void addBitmapToCache(final String key, final Bitmap bitmap) {
        if (getBitmapFromLRUCache(key) != null)
            return;

        designatedMemoryCache.put(key, bitmap);

        // Report cache size changes
        if (BuildConfig.DEBUG && DEBUG) {
            // Calculate how long it took for the Cache to fill its memory by detecting its
            // first purge
            final float cacheSize = designatedMemoryCache.size() / 1024.f;
            final float cacheSizeDelta = cacheSize - globalCacheCurrentSize;
            if (cacheSizeDelta < 0 && timeBeforeFirstCachePurge > Short.MAX_VALUE) {
                timeBeforeFirstCachePurge = System.currentTimeMillis() - timeBeforeFirstCachePurge;
                Log.w(TAG, String.format(
                    "Cache hit max size in %.2fmin and will start purging from now on (slower performance)",
                    (float)timeBeforeFirstCachePurge / (1000.f * 60.f)));
            }

            Log.d(TAG, String.format("Current cache size: %.2f/%.2fmb | delta: %.2fmb | isGlobal: %s", cacheSize, designatedMemoryCache
                    .maxSize() / 1024.f, cacheSizeDelta, isUsingGlobalCache() ? "yes" : "no"));

            globalCacheCurrentSize = cacheSize;
        }
    }

    private Bitmap getBitmapFromLRUCache(final String urlString) {
        final Bitmap cachedBitmap = designatedMemoryCache.get(urlString);

        if (cachedBitmap == null)
            return null;

        if (LOG_CACHE_OPERATIONS)
            Log.v(TAG, "Item loaded from LRU cache: " + urlString);

        return cachedBitmap;
    }

    private Bitmap getBitmapFromDiskCache(final String urlString) {
        final String key = CacheManager.sanitizeUrl(urlString);
        final Bitmap cachedBitmap = cacheManager.getDiskCache().getBitmap(key);

        if (cachedBitmap == null)
            return null;

        if (LOG_CACHE_OPERATIONS) {
            Log.v(TAG, "image read from Disk cache: " + key);
        }

        return cachedBitmap;
    }

    private void queueJob(final String url) {
        BitmapHttpClient.get(url, null, new BinaryHttpResponseHandler() {
            @Override
            public void onSuccess(final byte[] binaryData) {
                if (binaryData == null) {
                    Log.e(TAG, "downloadBitmap got null binaryData");
                    return;
                }

                final Bitmap bitmap = BitmapProcessor.decodeByteArray(binaryData, null);

                if (bitmap == null) {
                    Log.e(TAG, "downloadBitmap got null bitmap");
                    return;
                }

                addBitmapToCache(url, bitmap);

                if (bitmapCallback != null) {
                    bitmapCallback.onBitmapLoaded(bitmap);
                }
            }
        });

        if (LOG_CACHE_OPERATIONS) {
            Log.d(TAG, "Image downloaded: " + url);
        }
    }

    private void queueJob(final String url, final ImageView imageView, final JobOptions options) {
        final BitmapProcessor processor = new BitmapProcessor(context);

        processor.decodeSampledBitmapFromUrl(url, options.requestedWidth, options.requestedHeight, new BitmapProcessorCallback() {
            @Override
            public void onSuccess(final Bitmap bitmap) {
                if (bitmap == null) {
                    Log.e(TAG, "downloadDrawable got null");
                    return;
                }

                addBitmapToCache(url, bitmap);

                final String cachedUrl = runningJobs.get(imageView);

                if (cachedUrl != null && cachedUrl.equals(url)) {
                    options.fadeIn = true;
                    setImageDrawable(imageView, bitmap, options, LoadedFrom.NETWORK);
                }
            }

            @Override
            public void onFailure(final Throwable error, final String content) {
                if (placeholderDrawable != null) {
                    imageView.setImageDrawable(placeholderDrawable);
                }

                Log.e(TAG, String.format("failed to download %s: %s", url, content), error);
            }
        });
    }

    /**
     * Loads an image from the provided URL and caches it in the manager's LRU and Disk cache.
     * If the image is already cached, fetches it from the cache instead.
     * Calls the BitmapCallback upon completion, with the loaded Bitmap instance.
     * 
     * @param urlString the URL string to load the image from
     */
    public void loadImage(final String urlString) {
        if (urlString == null || urlString == "") {
            return;
        }

        final Bitmap bitmapFromCache = getBitmapFromCache(urlString);

        if (bitmapFromCache != null) {
            if (bitmapCallback != null) {
                bitmapCallback.onBitmapLoaded(bitmapFromCache);
            }
            return;
        }

        queueJob(urlString);
    }

    private void setImageDrawable(final ImageView imageView, Bitmap bitmap, final JobOptions options, final LoadedFrom loadedFrom) {
        if (options.roundedCorners) {
            // TODO: Optimize - @felipecsl: ideas?
            final BitmapProcessor processor = new BitmapProcessor(context);
            bitmap = processor.getRoundedCorners(bitmap, options.cornerRadius);
        }

        if (bitmap != null) {
            // If we didn't get OutOfMemoryError in getRoundedCorners()
            int targetWidth = imageView.getMeasuredWidth();
            int targetHeight = imageView.getMeasuredHeight();
            if (targetWidth != 0 && targetHeight != 0) {
                options.requestedWidth = targetWidth;
                options.requestedHeight = targetHeight;
            }
            CacheableDrawable.setBitmap(
                imageView,
                context,
                transformResult(options, bitmap),
                loadedFrom,
                !options.fadeIn,
                BuildConfig.DEBUG);
        }

        if (imageViewCallback != null) {
            imageViewCallback.onImageLoaded(imageView, bitmap);
        }
    }

    public void setImageViewCallback(final ImageViewCallback callback) {
        imageViewCallback = callback;
    }

    public void setBitmapCallback(final BitmapCallback bitmapCallback) {
        this.bitmapCallback = bitmapCallback;
    }

    // Took from
    // https://github.com/square/picasso/blob/master/picasso/src/main/java/com/squareup/picasso/Picasso.java
    private static Bitmap transformResult(JobOptions options, Bitmap result) {
        int inWidth = result.getWidth();
        int inHeight = result.getHeight();

        int drawX = 0;
        int drawY = 0;
        int drawWidth = inWidth;
        int drawHeight = inHeight;

        Matrix matrix = new Matrix();

        if (options != null) {
            int targetWidth = options.requestedWidth;
            int targetHeight = options.requestedHeight;

            if (options.centerCrop) {
                float widthRatio = targetWidth / (float)inWidth;
                float heightRatio = targetHeight / (float)inHeight;
                float scale;
                if (widthRatio > heightRatio) {
                    scale = widthRatio;
                    int newSize = (int)Math.ceil(inHeight * (heightRatio / widthRatio));
                    drawY = (inHeight - newSize) / 2;
                    drawHeight = newSize;
                } else {
                    scale = heightRatio;
                    int newSize = (int)Math.ceil(inWidth * (widthRatio / heightRatio));
                    drawX = (inWidth - newSize) / 2;
                    drawWidth = newSize;
                }
                matrix.preScale(scale, scale);

                Bitmap newResult = null;

                try {
                    newResult = Bitmap.createBitmap(result, drawX, drawY, drawWidth, drawHeight, matrix, false);
                } catch (final OutOfMemoryError e) {
                    Log.e(TAG, "Out of memory in transformResult()");

                    // If failed to allocate new bitmap, just return the original
                    return result;
                }

                if (newResult != result) {
                    result = newResult;
                }
            }
        }
        return result;
    }
}
