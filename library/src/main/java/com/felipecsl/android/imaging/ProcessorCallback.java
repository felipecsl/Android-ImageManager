package com.felipecsl.android.imaging;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import com.felipecsl.android.imaging.ImageManager.ImageViewCallback;

public class ProcessorCallback implements ImageManagerCallback {

    private static final String TAG = "ImageManagerBitmapProcessorCallback";
    private final String url;
    private final ImageView imageView;
    private final JobOptions options;
    private final ImageManager imageManager;

    private static final Handler uiHandler = new Handler(Looper.getMainLooper());

    public ProcessorCallback(final ImageManager imageManager, final String url, final ImageView imageView, final JobOptions options) {
        this.imageManager = imageManager;
        this.url = url;
        this.imageView = imageView;
        this.options = options;
    }

    @Override
    public void onBitmapLoaded(final Bitmap bitmap, final LoadedFrom source) {
        if (bitmap == null) {
            Log.e(TAG, "queueJob for urlString null");
            return;
        }

        imageManager.getCacheManager().put(ImageManager.getCacheKeyForJob(url, options), bitmap);

        final String cachedUrl = imageManager.getRunningJobs().get(imageView);

        if (cachedUrl != null && cachedUrl.equals(url)) {
            options.fadeIn = true;
            setImageDrawable(imageView, bitmap, options, source);
        }
    }

    @Override
    public void onLoadFailed(final LoadedFrom source, final Exception e) {
        if (imageManager.getPlaceholderResId() != ImageManager.NO_PLACEHOLDER) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    CacheableDrawable.setPlaceholder(imageView, imageManager.getPlaceholderResId(), null);
                }
            });
        }

        Log.e(TAG, String.format("failed to load %s: %s from %s", url, e.getMessage(), source.toString()), e);
    }

    private void setImageDrawable(final ImageView imageView, Bitmap bitmap, final JobOptions options, final LoadedFrom loadedFrom) {
        final int targetWidth = imageView.getMeasuredWidth();
        final int targetHeight = imageView.getMeasuredHeight();
        if (targetWidth != 0 && targetHeight != 0) {
            options.requestedWidth = targetWidth;
            options.requestedHeight = targetHeight;
        }

        // Process the transformed (smaller) image
        final BitmapProcessor processor = new BitmapProcessor(imageManager.getContext());
        Bitmap processedBitmap = null;

        if (options.roundedCorners)
            processedBitmap = processor.getRoundedCorners(bitmap, options.radius);
        else if (options.circle)
            processedBitmap = processor.getCircle(bitmap);

        if (processedBitmap != null)
            bitmap = processedBitmap;

        final Bitmap finalBitmap = bitmap;

        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                CacheableDrawable.setBitmap(imageView, imageManager.getContext(), finalBitmap, loadedFrom, !options.fadeIn, true);

                final ImageViewCallback imageViewCallback = imageManager.getImageViewCallback();

                if (imageViewCallback != null)
                    imageViewCallback.onImageLoaded(imageView, finalBitmap);
            }
        });
    }
}
