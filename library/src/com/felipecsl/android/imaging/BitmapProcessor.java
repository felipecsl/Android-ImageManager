package com.felipecsl.android.imaging;

import java.io.InputStream;
import java.net.ResponseCache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.felipecsl.android.Utils;
import com.loopj.android.http.BinaryHttpResponseHandler;

public class BitmapProcessor {

    public static interface BitmapProcessorCallback {
        public void onSuccess(Bitmap bitmap);

        public void onFailure(Throwable error, String content);
    }

    private static final String TAG = "BitmapProcessor";
    private final Context context;

    public BitmapProcessor(final Context context) {
        this.context = context;
        ResponseCache.setDefault(new ImageResponseCache(context.getCacheDir()));
    }

    public Bitmap getRoundedCorners(final Bitmap bitmap, final int radius) {
        Bitmap output = null;
        try {
            output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_4444);
        } catch (final OutOfMemoryError e) {
            Log.e(TAG, "Out of memory in getRoundedCorners()");
            return null;
        }
        final Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public Bitmap getAvatarThumbnail(final Bitmap b) {
        Bitmap finalBitmap;
        if (b.getHeight() > b.getWidth()) {
            finalBitmap = Bitmap.createBitmap(b, 0, (b.getHeight() - b.getWidth()) / 2, b.getWidth(), b.getWidth());
        } else if (b.getHeight() < b.getWidth()) {
            finalBitmap = Bitmap.createBitmap(b, (b.getWidth() - b.getHeight()) / 2, 0, b.getHeight(), b.getHeight());
        } else {
            finalBitmap = b;
        }
        finalBitmap = Bitmap.createScaledBitmap(finalBitmap, Utils.dpToPx(context, 50), Utils.dpToPx(context, 50), true);

        final int radius = (int)(finalBitmap.getHeight() * 0.07f);

        return getRoundedCorners(finalBitmap, radius);
    }

    // Took from:
    // http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
    private static int calculateInSampleSize(final BitmapFactory.Options options, final int reqWidth, final int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if ((height > reqHeight) || (width > reqWidth)) {

            // Calculate ratios of height and width to requested height and
            // width
            final int heightRatio = Math.round((float)height / (float)reqHeight);
            final int widthRatio = Math.round((float)width / (float)reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will
            // guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = Math.min(heightRatio, widthRatio);
        }

        return inSampleSize;
    }

    public static Bitmap decodeStream(final InputStream stream) {
        try {
            return BitmapFactory.decodeStream(stream);
        } catch (final OutOfMemoryError e) {
            Log.e(TAG, "Out of memory in decodeStream()");
            return null;
        }
    }

    public static Bitmap decodeByteArray(final byte[] data, final BitmapFactory.Options options) {
        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (final OutOfMemoryError e) {
            Log.e(TAG, "Out of memory in decodeByteArray()");
            return null;
        }
    }

    /**
     * Decodes a sampled Bitmap from the provided url in the requested width and height
     * 
     * @param urlString URL to download the bitmap from
     * @param reqWidth Requested width
     * @param reqHeight Requested height
     * @return Decoded bitmap
     */
    public void decodeSampledBitmapFromUrl(
        final String urlString,
        final int reqWidth,
        final int reqHeight,
        final BitmapProcessorCallback callback) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPurgeable = true;
        options.inDither = false;
        options.inInputShareable = true;

        BitmapHttpClient.get(urlString, null, new BinaryHttpResponseHandler() {
            @Override
            public void onSuccess(final byte[] binaryData) {
                if (binaryData == null)
                    return;

                decodeByteArray(binaryData, options);

                int width = reqWidth;
                int height = reqHeight;

                if (reqWidth == 0) {
                    width = options.outWidth;
                }
                if (reqHeight == 0) {
                    height = options.outHeight;
                }

                // Calculate inSampleSize
                options.inSampleSize = calculateInSampleSize(options, width, height);

                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;

                BitmapHttpClient.get(urlString, null, new BinaryHttpResponseHandler() {
                    @Override
                    public void onSuccess(final byte[] binaryData) {
                        if (ImageManager.LOG_CACHE_OPERATIONS) {
                            Log.d(TAG, "Image downloaded: " + urlString);
                        }
                        if (binaryData != null)
                            callback.onSuccess(decodeByteArray(binaryData, options));
                    }
                });
            }

            @Override
            public void onFailure(final Throwable error, final String content) {
                callback.onFailure(error, content);
            }
        });
    }
}
