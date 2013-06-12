package com.felipecsl.android;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.felipecsl.android.imaging.MemoryLruImageCache;

public class Utils {
    public static final int IO_BUFFER_SIZE = 8 * 1024;
    private static final String TAG = "Utils";

    @SuppressLint("NewApi")
    private static class HoneycombOrHigherUtils {
        public static int getSizeInBytes(Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    }

    @SuppressLint("NewApi")
    private static class GingerbreadOrHigherUtils {
        public static boolean isExternalStorageRemovable() {
            return Environment.isExternalStorageRemovable();
        }
    }

    public static int getSizeInBytes(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR2) {
            return HoneycombOrHigherUtils.getSizeInBytes(bitmap);
        }

        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    public static File getDiskCacheDir(final Context context, final String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final boolean externalCacheAvailable = (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) ||
                                               !Utils.isExternalStorageRemovable();

        String cachePath = null;

        if (externalCacheAvailable && Utils.getExternalCacheDir(context) != null) {
            cachePath = Utils.getExternalCacheDir(context).getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }

        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * Creates a LruCache<String, Bitmap> with a capacity of 1/8th of the total available device
     * memory.
     * 
     * @return LruCache<String, Bitmap>
     */
    public static MemoryLruImageCache createDefaultBitmapLruCache() {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int)(Runtime.getRuntime().maxMemory() / 1024); // TODO: We should use
                                                                              // ActivityManager.MemoryInfo's
                                                                              // availMem
        Log.d(TAG, "Runtime.getRuntime().maxMemory(): " + maxMemory + "kb");

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        Log.d(TAG, "Initializing LruCache with size " + cacheSize + "kb");

        return new MemoryLruImageCache(cacheSize);
    }

    public static int dpToPx(final Context context, final int dp) {
        // Get the screen's density scale
        final float scale = context.getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return (int)((dp * scale) + 0.5f);
    }

    public static boolean isExternalStorageRemovable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            GingerbreadOrHigherUtils.isExternalStorageRemovable();
        }
        return true;
    }

    public static File getExternalCacheDir(Context context) {
        if (hasExternalCacheDir()) {
            return context.getExternalCacheDir();
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }

    public static boolean hasExternalCacheDir() {
        return Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO;
    }
}
