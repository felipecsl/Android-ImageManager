package com.felipecsl.android.imaging;

import static android.graphics.Color.WHITE;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.widget.ImageView;

import com.felipecsl.android.BuildConfig;

// Took from
// https://github.com/square/picasso/blob/master/picasso/src/main/java/com/squareup/picasso/PicassoDrawable.java
public final class CacheableDrawable extends Drawable {
    // Only accessed from main thread.
    private static final Paint DEBUG_PAINT = new Paint();
    private static final float FADE_DURATION = 200f; // ms

    /**
     * Create or update the drawable on the target {@link ImageView} to display the supplied bitmap
     * image.
     */
    static void setBitmap(final ImageView target, final Context context, final Bitmap bitmap, final LoadedFrom loadedFrom, final boolean noFade) {
        final CacheableDrawable drawable = new CacheableDrawable(context, target.getDrawable(), bitmap, loadedFrom, noFade);
        target.setImageDrawable(drawable);
    }

    /**
     * Create or update the drawable on the target {@link ImageView} to display the supplied
     * placeholder image.
     */
    static void setPlaceholder(final ImageView target, final int placeholderResId, final Drawable placeholderDrawable) {
        if (placeholderResId != 0) {
            target.setImageResource(placeholderResId);
        } else {
            target.setImageDrawable(placeholderDrawable);
        }
    }

    private final float density;
    private final LoadedFrom loadedFrom;
    final BitmapDrawable image;

    Drawable placeholder;

    long startTimeMillis;
    boolean animating;

    public CacheableDrawable(final Context context, final Drawable placeholder, final Bitmap bitmap, final LoadedFrom loadedFrom, final boolean noFade) {
        final Resources res = context.getResources();

        density = res.getDisplayMetrics().density;

        this.loadedFrom = loadedFrom;

        image = new BitmapDrawable(res, bitmap);

        final boolean fade = loadedFrom != LoadedFrom.MEMORY && !noFade;
        if (fade) {
            this.placeholder = placeholder;
            animating = true;
            startTimeMillis = SystemClock.uptimeMillis();
        }
    }

    @Override
    public void draw(final Canvas canvas) {
        if (!animating) {
            image.draw(canvas);
        } else {
            if (placeholder != null) {
                placeholder.draw(canvas);
            }

            final float normalized = (SystemClock.uptimeMillis() - startTimeMillis) / FADE_DURATION;
            final int alpha = (int)(0xFF * normalized);

            if (normalized >= 1f) {
                animating = false;
                placeholder = null;
                image.draw(canvas);
            } else {
                image.setAlpha(alpha);
                image.draw(canvas);
                image.setAlpha(0xFF);
                invalidateSelf();
            }
        }

        if (BuildConfig.DEBUG) {
            drawDebugIndicator(canvas);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return image.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return image.getIntrinsicHeight();
    }

    @Override
    public void setAlpha(final int alpha) {
        if (placeholder != null) {
            placeholder.setAlpha(alpha);
        }
        image.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(final ColorFilter cf) {
        if (placeholder != null) {
            placeholder.setColorFilter(cf);
        }
        image.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return image.getOpacity();
    }

    @Override
    protected void onBoundsChange(final Rect bounds) {
        super.onBoundsChange(bounds);

        image.setBounds(bounds);
        if (placeholder != null) {
            // Center placeholder inside the image bounds
            setBounds(placeholder);
        }
    }

    private void setBounds(final Drawable drawable) {
        final Rect bounds = getBounds();

        final int width = bounds.width();
        final int height = bounds.height();
        final float ratio = (float)width / height;

        final int drawableWidth = drawable.getIntrinsicWidth();
        final int drawableHeight = drawable.getIntrinsicHeight();
        final float drawableRatio = (float)drawableWidth / drawableHeight;

        if (drawableRatio < ratio) {
            final float scale = (float)height / drawableHeight;
            final int scaledDrawableWidth = (int)(drawableWidth * scale);
            final int drawableLeft = bounds.left - (scaledDrawableWidth - width) / 2;
            final int drawableRight = drawableLeft + scaledDrawableWidth;
            drawable.setBounds(drawableLeft, bounds.top, drawableRight, bounds.bottom);
        } else {
            final float scale = (float)width / drawableWidth;
            final int scaledDrawableHeight = (int)(drawableHeight * scale);
            final int drawableTop = bounds.top - (scaledDrawableHeight - height) / 2;
            final int drawableBottom = drawableTop + scaledDrawableHeight;
            drawable.setBounds(bounds.left, drawableTop, bounds.right, drawableBottom);
        }
    }

    private void drawDebugIndicator(final Canvas canvas) {
        canvas.save();
        canvas.rotate(45);

        // Draw a white square for the indicator border.
        DEBUG_PAINT.setColor(WHITE);
        canvas.drawRect(0, -10 * density, 7.5f * density, 10 * density, DEBUG_PAINT);

        // Draw a slightly smaller square for the indicator color.
        DEBUG_PAINT.setColor(loadedFrom.debugColor);
        canvas.drawRect(0, -9 * density, 6.5f * density, 9 * density, DEBUG_PAINT);

        canvas.restore();

        final Paint paint = new Paint();
        paint.setColor(loadedFrom.debugColor);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(1 * density, 1 * density, canvas.getWidth(), canvas.getHeight(), paint);
    }
}
