package com.felipecsl.android.imaging;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

public class ImageUtil {
    private static final String TAG = "ImageUtils";

    /**
     * Returns the max size bounds (vertical/horizontal) possible in this device.
     * 
     * @return int pixels (512, 1024 or 2048)
     */
    public static int getMaxImageSizeBounds() {
        final long availMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        final int bounds = availMem > 15 ? 2048 : availMem > 5 ? 1024 : 512;
        return bounds;
    }

    /**
     * Returns one of these:
     * <p>
     * ExifInterface.ORIENTATION_NORMAL, ORIENTATION_ROTATE_90, ORIENTATION_ROTATE_180 or ORIENTATION_ROTATE_270
     * <p>
     * Or 0 if file is not Jpeg or is not found.
     * 
     * @param context
     * @param filePath
     * @return orientation
     */
    public static int getExifOrientationFromJpeg(final Context context, final String filePath) {
        try {
            final ExifInterface exif = new ExifInterface(filePath);
            return Integer.parseInt(exif.getAttribute(ExifInterface.TAG_ORIENTATION));

        } catch (final Exception e) {
            Log.e(TAG, "", e);
            return 0;
        }
    }

    public static Bitmap rotateBitmapToExifOrientation(final Context context, final String filePath, final Bitmap bitmap) {
        final int orientation = ImageUtil.getExifOrientationFromJpeg(context, filePath);
        final int degrees;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                degrees = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degrees = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degrees = 270;
                break;
            default:
                // Do nothing
                return bitmap;
        }

        final Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static boolean isJPG(final String uri) {
        return uri.contains(".jpg") || uri.contains(".jpeg");
    }

    /**
     * Taken from {@linkplain https://github.com/square/picasso/blob/master/picasso/src/main/java/com/squareup/picasso/Picasso.java}
     * 
     * @param result
     * @param options
     * @return transformed Bitmap
     */
    public static Bitmap transformBitmap(Bitmap bitmap, final JobOptions options) {

        // Adjust the bitmap to its bounds
        // This is very inefficient, but will only happen in 0.01% cases and not
        // during normal app execution (eg. Add Images->Bookmarklet->Try to preview huge image)
        // TODO: Find a more optimum way of doing this
        if (options.bounds > 0 && (bitmap.getWidth() > options.bounds || bitmap.getHeight() > options.bounds)) {
            final float aspect = (float)bitmap.getWidth() / (float)bitmap.getHeight();
            final int boundsWidth, boundsHeight;
            if (bitmap.getWidth() > bitmap.getHeight()) {
                final int delta = bitmap.getWidth() - options.bounds;
                boundsWidth = bitmap.getWidth() - delta;
                boundsHeight = bitmap.getHeight() - (int)Math.floor((delta / aspect) + .5f);
            } else {
                final int delta = bitmap.getHeight() - options.bounds;
                boundsWidth = bitmap.getWidth() - (int)Math.floor((delta * aspect) + .5f);
                boundsHeight = bitmap.getHeight() - delta;
            }

            bitmap = Bitmap.createScaledBitmap(bitmap, boundsWidth, boundsHeight, false);
        }

        // Transform
        final int inWidth = bitmap.getWidth();
        final int inHeight = bitmap.getHeight();

        int drawX = 0;
        int drawY = 0;
        int drawWidth = inWidth;
        int drawHeight = inHeight;

        final Matrix matrix = new Matrix();

        if (options != null) {
            int targetWidth = options.requestedWidth;
            int targetHeight = options.requestedHeight;

            if (options.scaleType != null && targetWidth != 0 && targetHeight != 0 && !(targetWidth == inWidth && targetHeight == inHeight)) {
                switch (options.scaleType) {
                    case CENTER_CROP: {
                        final float widthRatio = targetWidth / (float)inWidth;
                        final float heightRatio = targetHeight / (float)inHeight;
                        final float scale;
                        if (widthRatio > heightRatio) {
                            scale = widthRatio;
                            final int newSize = (int)Math.ceil(inHeight * (heightRatio / widthRatio));
                            drawY = (inHeight - newSize) / 2;
                            drawHeight = newSize;
                        } else {
                            scale = heightRatio;
                            final int newSize = (int)Math.ceil(inWidth * (widthRatio / heightRatio));
                            drawX = (inWidth - newSize) / 2;
                            drawWidth = newSize;
                        }
                        matrix.preScale(scale, scale);
                        break;
                    }
                    case FIT_CENTER:
                        final float aspect = (float)inWidth / (float)inHeight;
                        if (inWidth > inHeight) {
                            final int delta = inWidth - targetWidth;
                            targetWidth = inWidth - delta;
                            targetHeight = inHeight - (int)Math.floor((delta / aspect) + .5f);
                        } else {
                            final int delta = inHeight - targetHeight;
                            targetWidth = inWidth - (int)Math.floor((delta * aspect) + .5f);
                            targetHeight = inHeight - delta;
                        }
                        matrix.postScale((float)targetWidth / (float)inWidth, (float)targetHeight / (float)inHeight);
                        break;

                    case FIT_XY:
                        drawX = drawY = 0;
                        drawWidth = inWidth;
                        drawHeight = inHeight;
                        matrix.postScale((float)targetWidth / (float)inWidth, (float)targetHeight / (float)inHeight);
                        break;

                    default:
                        break;
                }
            }

            Bitmap newResult = null;

            try {
                newResult = Bitmap.createBitmap(bitmap, drawX, drawY, drawWidth, drawHeight, matrix, false);
            } catch (final OutOfMemoryError e) {
                Log.e(TAG, "Out of memory in transformResult()");

                // If failed to allocate new bitmap, just return the original
                return bitmap;
            }

            if (newResult != bitmap) {
                bitmap = newResult;
            }

        }

        return bitmap;
    }
}
