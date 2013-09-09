package com.felipecsl.android.imaging.sample;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.felipecsl.android.Utils;
import com.felipecsl.android.imaging.ImageManager;
import com.felipecsl.android.imaging.JobOptions;
import com.felipecsl.android.imaging.ScaleType;

public class ListAdapter extends BaseAdapter {

    private static final float IMAGE_SIZE_RATIO = 0.8f;
    private final List<String> urls;
    private final ImageManager imageManager;
    private final Context context;
    private static int imgWidth;
    private static int imgHeight;
    private final JobOptions options;

    public ListAdapter(final Context context, final List<String> urls) {
        this.context = context;
        imageManager = new ImageManager(context);
        imageManager.setPlaceholderResId(R.color.placeholder);
        this.urls = urls;
        imgHeight = Utils.dpToPx(context, 200);
        options = new JobOptions();
        options.scaleType = ScaleType.CENTER_CROP;
        options.requestedHeight = imgHeight;
        options.requestedWidth = imgWidth;
    }

    @SuppressLint("NewApi")
    private int getImageHeight(final GridView gridView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            return (int)(gridView.getColumnWidth() * IMAGE_SIZE_RATIO);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            return (int)((getScreenWidth() / gridView.getNumColumns()) * IMAGE_SIZE_RATIO);

        return imgHeight;
    }

    public int getScreenWidth() {
        if (context == null)
            return 0;

        return getDisplayMetrics().widthPixels;
    }

    public DisplayMetrics getDisplayMetrics() {
        final WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        final DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics;
    }

    @Override
    public int getCount() {
        return urls.size();
    }

    @Override
    public Object getItem(final int i) {
        return urls.get(i);
    }

    @Override
    public long getItemId(final int i) {
        return 0;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        ImageView imageView = null;

        if (convertView == null) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, getImageHeight((GridView)parent)));
        } else {
            imageView = (ImageView)convertView;
        }

        imageManager.loadImage(urls.get(position), imageView, options);

        return imageView;
    }
}
