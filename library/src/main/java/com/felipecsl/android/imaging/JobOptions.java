package com.felipecsl.android.imaging;

import android.widget.ImageView;

public class JobOptions {
    public boolean roundedCorners = false;
    public boolean circle = false;
    public boolean fadeIn = false;

    // default: no scaling
    public ScaleType scaleType = ScaleType.NONE;

    public int radius = 5;
    public int requestedWidth;
    public int requestedHeight;

    // size bounds, 1024 or 2048, to avoid loading big images to imageViews
    public int bounds;

    public JobOptions() {
        this(0, 0);
    }

    public JobOptions(final ImageView imgView) {
        this(imgView.getWidth(), imgView.getHeight());
    }

    public JobOptions(final int requestedWidth, final int requestedHeight) {
        this.requestedWidth = requestedWidth;
        this.requestedHeight = requestedHeight;
    }
}
