package com.felipecsl.android.imaging;

/**
 * Matches ImageView.ScaleType but only with supported scale types.
 */
public enum ScaleType {
    NONE(-1), FIT_XY(1), FIT_CENTER(3), CENTER_CROP(6);

    ScaleType(final int ni) {
        nativeInt = ni;
    }

    final int nativeInt;
}
