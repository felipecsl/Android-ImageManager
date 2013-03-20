package com.felipecsl.android;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;

public class Utils {
    public static final int IO_BUFFER_SIZE = 8 * 1024;
    
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
        // Check if media is mounted or storage is built-in, if so, try and use
        // external cache dir
        // otherwise use internal cache dir
        final String cachePath = (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) || !Utils.isExternalStorageRemovable() 
                ? Utils.getExternalCacheDir(context).getPath() 
                : context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    public static void fadeIn(View view) {
        view.clearAnimation();
        final int fadeInDuration = 200; // 0.2s

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator()); // add this
        fadeIn.setDuration(fadeInDuration);

        AnimationSet animation = new AnimationSet(false);
        animation.addAnimation(fadeIn);
        animation.setRepeatCount(1);
        view.setAnimation(animation);
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
