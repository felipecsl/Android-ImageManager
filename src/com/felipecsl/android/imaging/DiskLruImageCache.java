package com.felipecsl.android.imaging;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.felipecsl.android.Utils;
import com.jakewharton.DiskLruCache;

/**
 * Android Disk Lru Image Cache
 * Took from http://stackoverflow.com/questions/10185898/using-disklrucache-in-android-4-0-does-not-provide-for-opencache-method with adaptations
 * @author Felipe Lima
 *
 */
public class DiskLruImageCache {

    private DiskLruCache diskCache;
    
    private static final int IO_BUFFER_SIZE = 8 * 1024;
    private static final boolean LOG_CACHE_OPERATIONS = false;
    private static final String LOG_TAG = "DiskLruImageCache";
    private static CompressFormat mCompressFormat = CompressFormat.PNG;
    private static int mCompressQuality = 70;
    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;
    private static final String TAG = "DiskLruImageCache";
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB

    public DiskLruImageCache(Context context) {
        this(context, TAG, DISK_CACHE_SIZE, mCompressFormat, mCompressQuality);
    }

    public DiskLruImageCache(Context context, String uniqueName, int diskCacheSize, CompressFormat compressFormat, int quality) {
        try {
            final File diskCacheDir = getDiskCacheDir(context, uniqueName);
            diskCache = DiskLruCache.open(diskCacheDir, APP_VERSION, VALUE_COUNT, diskCacheSize);
            mCompressFormat = compressFormat;
            mCompressQuality = quality;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean writeBitmapToFile(Bitmap bitmap, DiskLruCache.Editor editor) throws IOException, FileNotFoundException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE);
            return bitmap.compress(mCompressFormat, mCompressQuality, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use
        // external cache dir
        // otherwise use internal cache dir
        final String cachePath = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED || !Utils.isExternalStorageRemovable() ? Utils.getExternalCacheDir(context).getPath()
                : context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    public void put(String key, Bitmap data) {

        DiskLruCache.Editor editor = null;
        try {
            editor = diskCache.edit(key);
            if (editor == null) {
                return;
            }

            if (writeBitmapToFile(data, editor)) {
                editor.commit();
                if (LOG_CACHE_OPERATIONS) {
                    Log.v(TAG, "image put on disk cache " + key);
                }
            } else {
                editor.abort();
                Log.e(TAG, "ERROR on: image put on disk cache " + key);
            }
        } catch (IOException e) {
            Log.e(TAG, "ERROR on: image put on disk cache " + key, e);
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (IOException ignored) {
            }
        }

    }

    public Bitmap getBitmap(String key) {
        Bitmap bitmap = null;
        DiskLruCache.Snapshot snapshot = null;
        try {

            snapshot = diskCache.get(key);
            if (snapshot == null) {
                return null;
            }
            final InputStream in = snapshot.getInputStream(0);
            if (in != null) {
                final BufferedInputStream buffIn = new BufferedInputStream(in, IO_BUFFER_SIZE);
                bitmap = BitmapFactory.decodeStream(buffIn);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }

        return bitmap;
    }

    public boolean containsKey(String key) {
        boolean contained = false;
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = diskCache.get(key);
            contained = snapshot != null;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }

        return contained;
    }

    public void clearCache() {
        if (LOG_CACHE_OPERATIONS) {
            Log.v(LOG_TAG, "disk cache CLEARED");
        }
        try {
            diskCache.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public File getCacheFolder() {
        return diskCache.getDirectory();
    }
}