package com.felipecsl.android.imaging;

import android.widget.ImageView;

public class JobOptions {
    public boolean roundedCorners = false;
    public boolean fadeIn = false;
    public ScaleType scaleType = ScaleType.NONE;    // default: no scaling
    public boolean storeTransformed = false;        // Store the transformed image (re-scaled) instead of the original (useful for huge images)
    public int cornerRadius = 5;
    public int requestedWidth;
    public int requestedHeight;
    public int bounds;                              // size bounds, 1024 or 2048, to avoid loading big images to imageViews

    public JobOptions() {
        this(0, 0);
    }

    public JobOptions(final ImageView imgView) {
        this(imgView.getWidth(), imgView.getHeight());
    }

    public JobOptions(final int requestedWidth, final int requestedHeight) {
        this.requestedWidth = requestedWidth;
        this.requestedHeight = requestedHeight;
        onPreExecute();
    }

    /**
     * @Override to set up the Options object
     */
    protected void onPreExecute() {};
}
