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

import com.felipecsl.android.imaging.MemoryLruImageCache.MemoryCacheEntryRemovedCallback;

import android.content.Context;
import android.graphics.Bitmap;

/**
 * public class <h1>CacheManager</h1> implements {@linkplain MemoryCacheEntryRemovedCallback}
 * 
 * <h2>Class Overview</h2>
 * Manages data within caches. 
 * 
 * @author Matias Pequeno
 * @version 0.1
 * @since 0.1
 * @see 
 */
public class CacheManager implements MemoryCacheEntryRemovedCallback {
    
    private final MemoryLruImageCache memoryCache;
    private final DiskLruImageCache diskCache;

    /**
     * 
     */
    public CacheManager(final Context context, final MemoryLruImageCache memoryCache, final DiskLruImageCache diskCache) {
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
    public void onEntryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
        if (oldValue == null || diskCache == null)
            return;

        // Add the just evicted memory cache entry to disk cache (2nd level cache)
        final String diskCacheKey = sanitizeUrl(key);

        if (!diskCache.containsKey(diskCacheKey))
            diskCache.put(diskCacheKey, oldValue);
    }

    public static String sanitizeUrl(final String url) {
        final String sanitizedKey = url.replaceAll("[^a-z0-9_]", "").replaceAll("httpdatawhicdncomimages", "");
        return sanitizedKey.substring(0, Math.min(63, sanitizedKey.length()));
    }

}
