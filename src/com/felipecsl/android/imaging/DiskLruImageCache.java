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
import com.jakewharton.DiskLruCache;

// Took from
// http://stackoverflow.com/questions/10185898/using-disklrucache-in-android-4-0-does-not-provide-for-opencache-method
// With adaptations
public class DiskLruImageCache {

    private static final String LOG_TAG = "DiskLruImageCache";
    private DiskLruCache mDiskCache;
    private static CompressFormat mCompressFormat = CompressFormat.JPEG;
    private static int mCompressQuality = 70;
    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;
    private static final String TAG = "WHIDiskLruImageCache";
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB

    public DiskLruImageCache(final Context context) {
        this(context, mCompressFormat, mCompressQuality);
    }

    public DiskLruImageCache(final Context context, final CompressFormat compressFormat, final int quality) {
        try {
            final File diskCacheDir = Utils.getDiskCacheDir(context, DiskLruImageCache.TAG);
            mDiskCache = DiskLruCache.open(diskCacheDir, APP_VERSION, VALUE_COUNT, DiskLruImageCache.DISK_CACHE_SIZE);
            mCompressFormat = compressFormat;
            mCompressQuality = quality;
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private boolean writeBitmapToFile(final Bitmap bitmap, final DiskLruCache.Editor editor) throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(editor.newOutputStream(0), Utils.IO_BUFFER_SIZE);
            return bitmap.compress(mCompressFormat, mCompressQuality, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }



    @SuppressWarnings("unused")
    public void put(final String key, final Bitmap data) {

        DiskLruCache.Editor editor = null;
        try {
            editor = mDiskCache.edit(key);
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
            } catch (final IOException ignored) {}
        }

    }

    public Bitmap getBitmap(final String key) {
        Bitmap bitmap = null;
        DiskLruCache.Snapshot snapshot = null;
        try {

            snapshot = mDiskCache.get(key);
            if (snapshot == null)
                return null;
            final InputStream in = snapshot.getInputStream(0);
            if (in != null) {
                final BufferedInputStream buffIn = new BufferedInputStream(in, Utils.IO_BUFFER_SIZE);
                bitmap = BitmapProcessor.decodeStream(buffIn);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }

        return bitmap;
    }


    public boolean containsKey(final String key) {
        boolean contained = false;
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = mDiskCache.get(key);
            contained = (snapshot != null);
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }

        return contained;
    }

    public void clearCache() {
        if (ImageManager.LOG_CACHE_OPERATIONS) {
            Log.v(LOG_TAG, "disk cache CLEARED");
        }
        try {
            mDiskCache.delete();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

}
