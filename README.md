Android-ImageManager
====================

Android-ImageManager handles all the boilerplate code for simple downloading caching and using images with Android.
It downloads the images and caches them using an LRU and disk cache.

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

        new InitDiskCacheTask() {
			@Override
			protected void onPostExecute(DiskLruImageCache diskCache) {
				imageManager = new ImageManager(diskCache, self);
			}
		}.execute(this);
	}
}
```

**Loading images**

```java
ImageManagerOptions options = new ImageManagerOptions();
imageManager.loadImage("http://www.roflcat.com/images/cats/bike.jpg", imageView);
```

This library was tested with Android API Level 8 and newer.

## Contributing to Android-ImageManager

 * Check out the latest master to make sure the feature hasn't been implemented or the bug hasn't been fixed yet
 * Check out the issue tracker to make sure someone already hasn't requested it and/or contributed it
 * Fork the project
 * Start a feature/bugfix branch
 * Commit and push until you are happy with your contribution
 * Make sure to add tests for it. This is important so I don't break it in a future version unintentionally.

## Contributors

 * Felipe Lima ([@felipecsl](https://github.com/felipecsl))

## Copyright

Copyright (c) 2012 Felipe Lima. See LICENSE.txt for further details.