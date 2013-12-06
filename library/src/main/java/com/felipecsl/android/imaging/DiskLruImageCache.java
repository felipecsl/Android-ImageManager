package com.felipecsl.android.imaging;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

import com.felipecsl.android.BuildConfig;
import com.felipecsl.android.Utils;
import com.jakewharton.disklrucache.DiskLruCache;

// Took from
// http://stackoverflow.com/questions/10185898/using-disklrucache-in-android-4-0-does-not-provide-for-opencache-method
// With adaptations
public class DiskLruImageCache {

    private static final String TAG = "DiskLruImageCache";
    private DiskLruCache diskCache;
    private static DiskLruImageCache instance;
    private static CompressFormat compressFormat = CompressFormat.JPEG;
    private static int compressQuality = 70;
    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB

    public static DiskLruImageCache getInstance(final Context context) {
        if (instance == null) {
            instance = new DiskLruImageCache(context);
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    private DiskLruImageCache(final Context context) {
        this(context, compressFormat, compressQuality);
    }

    private DiskLruImageCache(final Context context, final CompressFormat format, final int quality) {
        try {
            final File diskCacheDir = Utils.getDiskCacheDir(context, DiskLruImageCache.TAG);
            diskCache = DiskLruCache.open(diskCacheDir, APP_VERSION, VALUE_COUNT, DiskLruImageCache.DISK_CACHE_SIZE);
            compressFormat = format;
            compressQuality = quality;
        } catch (final IOException e) {
            Log.e(TAG, "Failed to initialize DiskLruImageCache", e);
        }
    }

    private boolean writeBitmapToFile(final Bitmap bitmap, final DiskLruCache.Editor editor) throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(editor.newOutputStream(0), Utils.IO_BUFFER_SIZE);
            return bitmap.compress(compressFormat, compressQuality, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    @SuppressWarnings("unused")
    public void put(final String key, final Bitmap data) {
        if (diskCache == null) {
            return;
        }

        DiskLruCache.Editor editor = null;
        try {
            editor = diskCache.edit(key);
            if (editor == null)
                return;

            if (writeBitmapToFile(data, editor)) {
                editor.commit();
                if (BuildConfig.DEBUG && ImageManager.LOG_CACHE_OPERATIONS) {
                    Log.v(TAG, "image put on disk cache " + key);
                }
            } else {
                editor.abort();
                Log.e(TAG, "ERROR on: image put on disk cache " + key);
            }
        } catch (final IOException e) {
            Log.e(TAG, "ERROR on: image put on disk cache " + key, e);
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (final IOException ignored) {} catch (final IllegalStateException ignored) {}
        }

    }

    public Bitmap getBitmap(final String key) {
        if (diskCache == null) {
            return null;
        }

        Bitmap bitmap = null;
        DiskLruCache.Snapshot snapshot = null;
        try {

            snapshot = diskCache.get(key);
            if (snapshot == null)
                return null;
            final InputStream in = snapshot.getInputStream(0);
            if (in != null) {
                final BufferedInputStream buffIn = new BufferedInputStream(in, Utils.IO_BUFFER_SIZE);
                bitmap = BitmapProcessor.decodeStream(buffIn);
            }
        } catch (final IOException e) {
            Log.e(TAG, "ERROR getBitmap", e);
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }

        return bitmap;
    }

    public boolean containsKey(final String key) {
        if (diskCache == null) {
            return false;
        }

        boolean contained = false;
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = diskCache.get(key);
            contained = (snapshot != null);
        } catch (final IOException e) {
            Log.e(TAG, "", e);
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }

        return contained;
    }

    @SuppressWarnings("unused")
    public void clearCache() {
        if (diskCache == null) {
            return;
        }

        if (BuildConfig.DEBUG && ImageManager.LOG_CACHE_OPERATIONS) {
            Log.v(TAG, "disk cache CLEARED");
        }
        try {
            diskCache.delete();
        } catch (final IOException e) {
            Log.e(TAG, "", e);
        }
    }

}
