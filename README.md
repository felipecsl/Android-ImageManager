Android-ImageManager
====================

Android-ImageManager handles all the boilerplate code for simple downloading caching and using images with Android.

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