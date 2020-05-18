package cn.scyutao.yutaohttp.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import android.util.LruCache

import com.android.volley.toolbox.ImageLoader
import com.jakewharton.disklrucache.DiskLruCache

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.SoftReference
import java.util.Collections
import java.util.HashSet

class LRUCache

(private val mImageLoaderConfig: ImageLoaderConfig) : ImageLoader.ImageCache {
    private val mCache: LruCache<String, Bitmap>
    private var mDiskLruCache: DiskLruCache? = null
    private val mDiskCacheLock = Object()
    private var mDiskCacheStarting = true
    private var mReusableBitmaps: MutableSet<SoftReference<Bitmap>>? = null

    init {
        if (Utils.hasHoneycomb()) {
            mReusableBitmaps = Collections.synchronizedSet(HashSet<SoftReference<Bitmap>>())
        }

        mCache = object : LruCache<String, Bitmap>(mImageLoaderConfig.memoryCacheSize / 1024) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                val bitmapSize = Utils.getBitmapSize(bitmap) / 1024
                return if (bitmapSize == 0) 1 else bitmapSize
            }
        }
        InitDiskCacheTask().execute()
    }

    private fun initDiskCache() {
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache == null || mDiskLruCache!!.isClosed) {
                val diskCacheDir = mImageLoaderConfig.diskCacheDir
                if (mImageLoaderConfig.diskCacheEnabled && diskCacheDir != null) {
                    if (!diskCacheDir.exists()) {
                        diskCacheDir.mkdirs()
                    }
                    if (Utils.getUsableSpace(diskCacheDir) > mImageLoaderConfig.diskCacheSize) {
                        try {
                            mDiskLruCache = DiskLruCache.open(
                                    diskCacheDir, 1, 1, mImageLoaderConfig.diskCacheSize.toLong())
                        } catch (e: IOException) {
                            Log.e(TAG, "initDiskCache - " + e)
                        }

                    }
                }
            }
            mDiskCacheStarting = false
            mDiskCacheLock.notifyAll()
        }
    }

    internal inner class InitDiskCacheTask : AsyncTask<File, Void, Void>() {
        override fun doInBackground(vararg params: File): Void? {
            initDiskCache()
            return null
        }
    }

    override fun getBitmap(url: String): Bitmap? {
        var bitmap = getBitmapFromMemCache(url)
        if (bitmap == null) {
            bitmap = getBitmapFromDiskCache(url)
        }
        return bitmap
    }

    override fun putBitmap(url: String?, bitmap: Bitmap?) {
        if (url == null || bitmap == null) {
            return
        }
        if (getBitmapFromMemCache(url) == null) {
            mCache.put(url, bitmap)
        }
        putBitmapToDiskCache(url, bitmap)
    }

    private fun putBitmapToDiskCache(url: String, bitmap: Bitmap) {

        synchronized (mDiskCacheLock) {

            if (mDiskLruCache != null) {
                val key = Utils.hashKeyForDisk(url)
                var out: OutputStream? = null
                try {
                    val snapshot = mDiskLruCache!!.get(key)
                    if (snapshot == null) {
                        val editor = mDiskLruCache!!.edit(key)
                        if (editor != null) {
                            out = editor.newOutputStream(DISK_CACHE_INDEX)
                            bitmap.compress(
                                    mImageLoaderConfig.compressFormat, mImageLoaderConfig.compressQuality, out)
                            editor.commit()
                            out!!.close()
                        }
                    } else {
                        snapshot.getInputStream(DISK_CACHE_INDEX).close()
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "addBitmapToCache - " + e)
                } catch (e: Exception) {
                    Log.e(TAG, "addBitmapToCache - " + e)
                } finally {
                    try {
                        if (out != null) {
                            out.close()
                        }
                    } catch (e: IOException) {
                    }

                }
            }
        }
    }

    private fun getBitmapFromMemCache(key: String): Bitmap? {
        return mCache.get(key)
    }

    private fun getBitmapFromDiskCache(url: String): Bitmap? {
        val key = Utils.hashKeyForDisk(url)
        var bitmap: Bitmap? = null

        synchronized (mDiskCacheLock) {
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait()
                } catch (e: InterruptedException) {
                }

            }
            if (mDiskLruCache != null) {
                var inputStream: InputStream? = null
                try {
                    val snapshot = mDiskLruCache!!.get(key)
                    if (snapshot != null) {
                        inputStream = snapshot.getInputStream(DISK_CACHE_INDEX)
                        if (inputStream != null) {
                            val fd = (inputStream as FileInputStream).fd
                            bitmap = Utils.decodeSampledBitmapFromDescriptor(
                                fd, Integer.MAX_VALUE, Integer.MAX_VALUE, this
                            )
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "getBitmapFromDiskCache - " + e)
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close()
                        }
                    } catch (e: IOException) {
                    }

                }
            }
            return bitmap
        }
    }

    internal fun getBitmapFromReusableSet(options: BitmapFactory.Options): Bitmap? {
        var bitmap: Bitmap? = null

        if (mReusableBitmaps != null && !mReusableBitmaps!!.isEmpty()) {
            synchronized (mReusableBitmaps!!) {
                val iterator = mReusableBitmaps!!.iterator()
                var item: Bitmap?

                while (iterator.hasNext()) {
                    item = iterator.next().get()

                    if (null != item && item.isMutable) {
                        if (Utils.canUseForInBitmap(item, options)) {
                            bitmap = item
                            iterator.remove()
                            break
                        }
                    } else {
                        iterator.remove()
                    }
                }
            }
        }

        return bitmap
    }

    companion object {
        private val TAG = "LRUCache"
        private val DISK_CACHE_INDEX = 0
    }

}
