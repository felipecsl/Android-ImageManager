package com.felipecsl.android.imaging;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.felipecsl.android.Utils;

public class MemoryLruImageCache extends LruCache<String, Bitmap> {

    private MemoryCacheEntryRemovedCallback onEntryRemovedCallback;

    public static interface MemoryCacheEntryRemovedCallback {
        void onEntryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue);
    }

    public MemoryLruImageCache(final int cacheSizeInKb) {
        super(cacheSizeInKb);
    }

    public void setEntryRemovedCallback(final MemoryCacheEntryRemovedCallback onEntryRemovedCallback) {
        this.onEntryRemovedCallback = onEntryRemovedCallback;
    }

    @Override
    protected int sizeOf(final String key, final Bitmap bitmap) {
        // The cache size will be measured in kilobytes rather than number of items.
        return Utils.getSizeInBytes(bitmap) / 1024;
    }

    @Override
    protected void entryRemoved(final boolean evicted, final String key, final Bitmap oldValue, final Bitmap newValue) {
        super.entryRemoved(evicted, key, oldValue, newValue);

        if (onEntryRemovedCallback != null) {
            onEntryRemovedCallback.onEntryRemoved(evicted, key, oldValue, newValue);
        }
    }
}
