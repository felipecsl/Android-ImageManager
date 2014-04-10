Android-ImageManager
====================

[DEPRECATED] Use [Picasso]((http://github.com/square/picasso)) instead 

Android-ImageManager handles all the boilerplate code for simple downloading, caching and using images with Android.
It downloads the images and caches them using an in-memory LRU and a second level Disk cache.

Usage:

**Initialization**

```java
import com.felipecsl.android.imaging.*;

public class MainActivity extends Activity {
	private ImageManager imageManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final MainActivity self = this;

        if (!DiskLruImageCache.isInitialized()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    DiskLruImageCache.getInstance(self);
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    imageManager = new ImageManager(self);
                }
            }.execute();
        }
}
```

**Loading images**

```java
imageManager.loadImage("http://www.roflcat.com/images/cats/bike.jpg", imageView, new JobOptions());
```

## Features

* Seamless two-level caching (Memory and Disk, using DiskLruCache)
* Concurrent image downloads via android-async-http library
* Auto-rotation of JPEG images based on the EXIF information
* Ability to load local files (eg. from the device Gallery app using Uri)

## Sample application

Check the [sample](https://github.com/felipecsl/Android-ImageManager/tree/master/samples) application for an example of usage

Try out the sample application on [Google Play](https://play.google.com/store/apps/details?id=com.felipecsl.android.imaging.sample)

[![Gplay](https://developer.android.com/images/brand/en_generic_rgb_wo_60.png)](https://play.google.com/store/apps/details?id=com.felipecsl.android.imaging.sample)

This library was tested with Android API Level 8 and newer.

## Applications using Android-ImageManager

 * [We Heart It](https://play.google.com/store/apps/details?id=com.weheartit)
 
If your app uses this library, send a pull request with the update to this Readme file adding your app to the list!

## Contributing to Android-ImageManager

 * Check out the latest master to make sure the feature hasn't been implemented or the bug hasn't been fixed yet
 * Check out the issue tracker to make sure someone already hasn't requested it and/or contributed it
 * Fork the project
 * Start a feature/bugfix branch
 * Commit and push until you are happy with your contribution
 * Make sure to add tests for it. This is important so I don't break it in a future version unintentionally.

## Contributors

 * Felipe Lima ([@felipecsl](https://github.com/felipecsl))
 * Matias Pequeno ([@matias-pequeno](https://github.com/matias-pequeno))

## Copyright

Copyright (c) 2012 Felipe Lima. See LICENSE.txt for further details.
