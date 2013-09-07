/**
 * CacheManager.java
 * weheartit for Android
 * 
 * Created by Matias Pequeno on Jun 03, 2013.
 * Copyright (C) 2013 WHI Inc. <www.weheartit.com>
 * 
 * This is proprietary software.
 * 
 * All rights reserved. No warranty, explicit or implicit, provided.
 * In no event shall the owner be liable for any claim or damages.
 * 
 */
package com.felipecsl.android.imaging;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.felipecsl.android.imaging.MemoryLruImageCache.MemoryCacheEntryRemovedCallback;

/**
 * public class <h1>CacheManager</h1> implements {@linkplain MemoryCacheEntryRemovedCallback}
 * 
 * <h2>Class Overview</h2>
 * Manages data within caches.
 * 
 * @author Matias Pequeno / Felipe Lima
 */
public class CacheManager implements MemoryCacheEntryRemovedCallback {

    private static final String TAG = "CacheManager";
    private final MemoryLruImageCache memoryCache;
    private final DiskLruImageCache diskCache;
    private static final HandlerThread handlerThread;
    private static final Handler diskCacheHandler;

    static {
        handlerThread = new HandlerThread("Cache Manager Disk Access Thread");
        handlerThread.start();
        diskCacheHandler = new Handler(handlerThread.getLooper());
    }

    public static interface CacheManagerCallback {
        void onBitmapLoaded(final Bitmap bitmap, final LoadedFrom source);
    }

    /**
     *
     */
    public CacheManager(final MemoryLruImageCache memoryCache, final DiskLruImageCache diskCache) {
        this.diskCache = diskCache;
        this.memoryCache = memoryCache;

        memoryCache.setEntryRemovedCallback(this);
    }

    public MemoryLruImageCache getMemoryCache() {
        return memoryCache;
    }

    public DiskLruImageCache getDiskCache() {
        return diskCache;
    }

    @Override
    public void onEntryRemoved(final boolean evicted, final String key, final Bitmap oldValue, final Bitmap newValue) {
        if (oldValue == null || diskCache == null)
            return;

        // Add the just evicted memory cache entry to disk cache (2nd level cache)
        final String diskCacheKey = sanitizeUrl(key);

        if (!diskCache.containsKey(diskCacheKey))
            diskCache.put(diskCacheKey, oldValue);
    }

    public void get(final String id, final CacheManagerCallback callback) {
        final Bitmap bitmap = getBitmapFromLRUCache(id);

        if (bitmap != null && callback != null) {
            callback.onBitmapLoaded(bitmap, LoadedFrom.MEMORY);
            return;
        }

        diskCacheHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onBitmapLoaded(getBitmapFromDiskCache(id), LoadedFrom.DISK);
                }
            }
        });
    }

    public void get(final String id) {
        get(id, null);
    }

    public void put(final String key, final Bitmap bitmap) {
        if (getBitmapFromLRUCache(key) != null)
            return;

        memoryCache.put(key, bitmap);
    }

    /** Private stuff **/

    private static String sanitizeUrl(final String url) {
        final String sanitizedKey = url.replaceAll("[^a-z0-9_]", "").replaceAll("httpdatawhicdncomimages", "");
        return sanitizedKey.substring(0, Math.min(63, sanitizedKey.length()));
    }

    private Bitmap getBitmapFromLRUCache(final String urlString) {
        final Bitmap cachedBitmap = memoryCache.get(urlString);

        if (cachedBitmap == null)
            return null;

        if (ImageManager.LOG_CACHE_OPERATIONS)
            Log.v(TAG, "Item loaded from LRU cache: " + urlString);

        return cachedBitmap;
    }

    private Bitmap getBitmapFromDiskCache(final String urlString) {
        if (diskCache == null)
            return null;

        final String key = sanitizeUrl(urlString);
        final Bitmap cachedBitmap = diskCache.getBitmap(key);

        if (cachedBitmap == null)
            return null;

        if (ImageManager.LOG_CACHE_OPERATIONS) {
            Log.v(TAG, "image read from Disk cache: " + key);
        }

        return cachedBitmap;
    }

    public void clear() {
        memoryCache.evictAll();
    }
}
