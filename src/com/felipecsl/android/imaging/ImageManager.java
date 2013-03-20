package com.felipecsl.android.imaging;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import com.felipecsl.android.BuildConfig;
import com.felipecsl.android.Utils;

public class ImageManager {

    /* Static members */
    public static final int NO_PLACEHOLDER = -1;
    
    public static final boolean LOG_CACHE_OPERATIONS = BuildConfig.DEBUG && false;

    /* Static members */
    private static final String LOG_TAG = "ImageManager";
    private static final Map<ImageView, String> imageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    private static LruCache<String, Bitmap> memoryCache;
    private static DiskLruImageCache diskCache;

    /* Instance members */
    private final ExecutorService downloadThreadPool;
    private final Context context;

    private ImageViewCallback imageViewCallback;
    private BitmapCallback bitmapCallback;
    private int placeholderResId;

    public interface ImageViewCallback {
        void onImageLoaded(ImageView imageView);
    }

    public interface BitmapCallback {
        void onBitmapLoaded(Bitmap bitmap);
    }

    public static class JobOptions {

        public boolean roundedCorners = false;
        public boolean fadeIn = true;
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

    public ImageManager(final DiskLruImageCache diskImageCache, final Context context) {
        this.context = context;

        if (diskCache == null) {
            diskCache = diskImageCache;
        }

        downloadThreadPool = Executors.newCachedThreadPool();

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int)(Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        if (memoryCache == null) {
            Log.d(LOG_TAG, "Initializing LruCache with size " + cacheSize + "KB");

            memoryCache = new LruCache<String, Bitmap>(cacheSize) {

                @Override
                protected int sizeOf(final String key, final Bitmap bitmap) {
                    // The cache size will be measured in bytes rather than
                    // number of items.
                    return Utils.getSizeInBytes(bitmap) / 1024;
                }
            };
        }

        // Default placeholder
        placeholderResId = Color.parseColor("#eeeeee");
    }

    /**
     * Loads an image from the provided URL and caches it in the manager's LRU and Disk cache.
     * If the image is already cached, fetches it from the cache instead. Upon completion,
     * assigns the loaded bitmap into the provided imageView object and calls the imageViewCallback
     * with the loaded ImageView object.
     * 
     * @param urlString the URL string to load the image from
     * @param imageView the ImageView that will receive the loaded image
     * @param options Load options
     */
    public void loadImage(final String urlString, final ImageView imageView, final JobOptions options) {
        imageViews.put(imageView, urlString);

        final Bitmap bitmap = getBitmapFromCache(urlString);

        if (bitmap != null) {
            final BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
            // we do not want to fade in if the image is already cached
            // to make things smoother
            options.fadeIn = false;
            setImageDrawable(imageView, drawable, options);
            return;
        }

        if (placeholderResId != NO_PLACEHOLDER)
            imageView.setImageResource(placeholderResId);
        queueJob(urlString, imageView, options);
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

    private Bitmap getBitmapFromCache(final String urlString) {
        final Bitmap bitmap = getBitmapFromLRUCache(urlString);

        if (bitmap != null)
            return bitmap;

        return getBitmapFromDiskCache(urlString);
    }

    private void addBitmapToCache(final String key, final Bitmap bitmap) {
        if (getBitmapFromLRUCache(key) == null) {
            memoryCache.put(key, bitmap);
        }

        final String diskCacheKey = getDiskCacheKey(key);

        if ((diskCache != null) && !diskCache.containsKey(diskCacheKey)) {
            diskCache.put(diskCacheKey, bitmap);
        }
    }


    private Bitmap getBitmapFromLRUCache(final String urlString) {
        final Bitmap cachedBitmap = memoryCache.get(urlString);

        if (cachedBitmap == null)
            return null;

        if (LOG_CACHE_OPERATIONS) {
            Log.v(LOG_TAG, "Item loaded from LRU cache: " + urlString);
        }

        return cachedBitmap;
    }


    private Bitmap getBitmapFromDiskCache(final String urlString) {
        final String key = getDiskCacheKey(urlString);
        final Bitmap cachedBitmap = diskCache.getBitmap(key);

        if (cachedBitmap == null)
            return null;

        if (LOG_CACHE_OPERATIONS) {
            Log.v(LOG_TAG, "image read from Disk cache: " + key);
        }

        return cachedBitmap;
    }

    private static String getDiskCacheKey(final String urlString) {
        final String sanitizedKey = urlString.replaceAll("[^a-z0-9_]", "");
        return sanitizedKey.substring(0, Math.min(63, sanitizedKey.length()));
    }

    private void queueJob(final String urlString) {
        downloadThreadPool.submit(new Runnable() {

            @Override
            public void run() {
                downloadBitmap(urlString);

                if (LOG_CACHE_OPERATIONS) {
                    Log.d(LOG_TAG, "Image downloaded: " + urlString);
                }
            }
        });
    }

    private void queueJob(final String urlString, final ImageView imageView, final JobOptions options) {
        final Handler handler = new ImageManagerHandler(this, imageView, urlString, options, placeholderResId);

        downloadThreadPool.submit(new Runnable() {

            @Override
            public void run() {
                final BitmapDrawable drawable = downloadDrawable(urlString, options);

                final Message message = handler.obtainMessage(1, drawable);

                if (LOG_CACHE_OPERATIONS) {
                    Log.d(LOG_TAG, "Image downloaded: " + urlString);
                }

                handler.sendMessage(message);
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
        final Bitmap bitmapFromCache = getBitmapFromCache(urlString);

        if (bitmapFromCache != null) {
            if (bitmapCallback != null) {
                bitmapCallback.onBitmapLoaded(bitmapFromCache);
            }
            return;
        }

        queueJob(urlString);
    }

    private void setImageDrawable(final ImageView imageView, BitmapDrawable bitmapDrawable, final JobOptions options) {
        if (options.roundedCorners) {
            final BitmapProcessor processor = new BitmapProcessor(context);
            bitmapDrawable = processor.getRoundedCorners(bitmapDrawable, options.cornerRadius);
        }

        imageView.setImageDrawable(bitmapDrawable);

        if (options.fadeIn) {
            Utils.fadeIn(imageView);
        }

        if (imageViewCallback != null) {
            imageViewCallback.onImageLoaded(imageView);
        }
    }

    private void downloadBitmap(final String urlString) {
        final BitmapConnection conn = new BitmapConnection();
        final Bitmap bitmap = conn.readStream(urlString, new BitmapConnection.Runnable<Bitmap>() {
            @Override
            public Bitmap run(final InputStream stream) {
                return BitmapProcessor.decodeStream(stream);
            }
        });

        if (bitmap == null) {
            Log.e(LOG_TAG, "downloadBitmap got null");
            return;
        }

        addBitmapToCache(urlString, bitmap);

        if (bitmapCallback != null) {
            bitmapCallback.onBitmapLoaded(bitmap);
        }
    }

    private BitmapDrawable downloadDrawable(final String urlString, final JobOptions options) {
        final BitmapProcessor processor = new BitmapProcessor(context);
        final Bitmap bitmap = processor.decodeSampledBitmapFromUrl(urlString, options.requestedWidth, options.requestedHeight);

        if (bitmap == null) {
            Log.e(LOG_TAG, "downloadDrawable got null");
            return null;
        }

        addBitmapToCache(urlString, bitmap);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public void setImageViewCallback(final ImageViewCallback callback) {
        imageViewCallback = callback;
    }

    public void setBitmapCallback(final BitmapCallback bitmapCallback) {
        this.bitmapCallback = bitmapCallback;
    }

    /**
     * Drawable Handler inner class
     * 
     */
    private static final class ImageManagerHandler extends Handler {

        private final ImageView imageView;
        private final String url;
        private final JobOptions options;
        private final WeakReference<ImageManager> imageManager;
        private final int placeholderResId;

        private ImageManagerHandler(final ImageManager imageManager, final ImageView imageView, final String url, final JobOptions options,
                final int placeholderResId) {
            this.imageManager = new WeakReference<ImageManager>(imageManager);
            this.imageView = imageView;
            this.url = url;
            this.options = options;
            this.placeholderResId = placeholderResId;
        }

        @Override
        public void handleMessage(final Message msg) {
            final String tag = imageViews.get(imageView);

            if ((tag != null) && tag.equals(url)) {
                if (msg.obj != null) {
                    imageManager.get().setImageDrawable(imageView, (BitmapDrawable)msg.obj, options);
                } else {
                    if (placeholderResId != NO_PLACEHOLDER)
                        imageView.setImageResource(placeholderResId);
                    Log.e(LOG_TAG, "failed " + url);
                }
            }
        }
    }
}
