package com.felipecsl.android.imaging.sample;

import java.util.List;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.felipecsl.android.Utils;
import com.felipecsl.android.imaging.ImageManager;
import com.felipecsl.android.imaging.ImageManager.JobOptions;

public class ListAdapter extends BaseAdapter {

    private List<String> urls;
    private final ImageManager imageManager;
    private final Context context;
    private static int imgWidth;
    private static int imgHeight;
    private final JobOptions options;

    public ListAdapter(Context context, List<String> urls) {
        this.context = context;
        imageManager = new ImageManager(context);
        this.urls = urls;
        imgHeight = Utils.dpToPx(context, 200);
        imgWidth = getScreenWidth(context);
        options = new JobOptions();
        options.centerCrop = true;
        options.requestedHeight = imgHeight;
        options.requestedWidth = imgWidth;
    }

    @Override
    public int getCount() {
        return urls.size();
    }

    @Override
    public Object getItem(int i) {
        return urls.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        ImageView imageView = null;

        if (convertView == null) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, imgHeight));
        } else {
            imageView = (ImageView)convertView;
        }

        imageManager.loadImage(urls.get(position), imageView, options);

        return imageView;
    }

    private static int getScreenWidth(final Context context) {
        if (context == null)
            return 0;
        return getDisplayMetrics(context).widthPixels;
    }

    private static DisplayMetrics getDisplayMetrics(final Context context) {
        final WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        final DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics;
    }
}
