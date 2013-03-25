package com.felipecsl.android.imaging;


import android.content.Context;
import android.os.AsyncTask;

public class InitDiskCacheTask extends AsyncTask<Context, Void, DiskLruImageCache> {

    public InitDiskCacheTask() {
    }

    @Override
    protected DiskLruImageCache doInBackground(Context... context) {
        return new DiskLruImageCache(context[0]);
    }
}
