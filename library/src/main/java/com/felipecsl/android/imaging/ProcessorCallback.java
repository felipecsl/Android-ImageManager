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
    public void onBitmapLoaded(Bitmap bitmap, final LoadedFrom source) {
        if (bitmap == null) {
            Log.e(TAG, "queueJob for urlString null");
            return;
        }

        if (options.storeTransformed)
            bitmap = ImageUtil.transformBitmap(bitmap, options);

        imageManager.getCacheManager().put(url, bitmap);

        final String cachedUrl = imageManager.getRunningJobs().get(imageView);

        if (cachedUrl != null && cachedUrl.equals(url)) {
            options.fadeIn = true;
            setImageDrawable(imageView, bitmap, options, source);
        }
    }

    @Override
    public void onLoadFailed(final LoadedFrom source, final Exception e) {
        if (imageManager.getPlaceholderResId() != ImageManager.NO_PLACEHOLDER)
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    CacheableDrawable.setPlaceholder(imageView, imageManager.getPlaceholderResId(), null);
                }
            });

        Log.e(TAG, String.format("failed to load %s: %s from %s", url, e.getMessage(), source.toString()), e);
    }

    private void setImageDrawable(final ImageView imageView, Bitmap bitmap, final JobOptions options, final LoadedFrom loadedFrom) {
        final int targetWidth = imageView.getMeasuredWidth();
        final int targetHeight = imageView.getMeasuredHeight();
        if (targetWidth != 0 && targetHeight != 0) {
            options.requestedWidth = targetWidth;
            options.requestedHeight = targetHeight;
        }

        // If storeTransformed is set, it means the bitmap was already transformed
        if (!options.storeTransformed)
            bitmap = ImageUtil.transformBitmap(bitmap, options);

        // Process the transformed (smaller) image
        if (options.roundedCorners) {
            final BitmapProcessor processor = new BitmapProcessor(imageManager.getContext());
            final Bitmap roundedBitmap = processor.getRoundedCorners(bitmap, options.cornerRadius);
            if (roundedBitmap != null)
                bitmap = roundedBitmap;
        }

        final Bitmap finalBitmap = bitmap;

        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                CacheableDrawable.setBitmap(imageView, imageManager.getContext(), finalBitmap, loadedFrom, !options.fadeIn);

                final ImageViewCallback imageViewCallback = imageManager.getImageViewCallback();

                if (imageViewCallback != null)
                    imageViewCallback.onImageLoaded(imageView, finalBitmap);
            }
        });
    }
}
