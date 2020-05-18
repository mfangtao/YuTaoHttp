package cn.scyutao.yutaohttp.image

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.os.StatFs
import android.os.StrictMode

import java.io.File
import java.io.FileDescriptor
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object Utils {
    @TargetApi(VERSION_CODES.HONEYCOMB)
    fun enableStrictMode() {
        if (hasGingerbread()) {
            val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog()
            val vmPolicyBuilder = StrictMode.VmPolicy.Builder().detectAll().penaltyLog()

            if (hasHoneycomb()) {
                threadPolicyBuilder.penaltyFlashScreen()
            }
            StrictMode.setThreadPolicy(threadPolicyBuilder.build())
            StrictMode.setVmPolicy(vmPolicyBuilder.build())
        }
    }

    fun hasFroyo(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.FROYO
    }

    fun hasGingerbread(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD
    }

    fun hasHoneycomb(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB
    }

    fun hasHoneycombMR1(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR1
    }

    fun hasJellyBean(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN
    }

    fun hasKitKat(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT
    }

    fun decodeSampledBitmapFromDescriptor(
            fileDescriptor: FileDescriptor, reqWidth: Int, reqHeight: Int, cache: LRUCache
    ): Bitmap {

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        options.inJustDecodeBounds = false

        if (hasHoneycomb()) {
            addInBitmapOptions(options, cache)
        }

        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    private fun addInBitmapOptions(options: BitmapFactory.Options, cache: LRUCache?) {

        options.inMutable = true

        if (cache != null) {
            val inBitmap = cache.getBitmapFromReusableSet(options)

            if (inBitmap != null) {
                options.inBitmap = inBitmap
            }
        }
    }

    fun calculateInSampleSize(options: BitmapFactory.Options,
                              reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth) {
                inSampleSize *= 2
            }

            var totalPixels = (width * height / inSampleSize).toLong()

            val totalReqPixelsCap = reqWidth * reqHeight * 2.toLong()

            while (totalPixels > totalReqPixelsCap) {
                inSampleSize *= 2
                totalPixels /= 2
            }
        }
        return inSampleSize
    }

    @TargetApi(VERSION_CODES.KITKAT)
    fun getBitmapSize(bitmap: Bitmap): Int {
        if (hasKitKat()) {
            return bitmap.allocationByteCount
        }

        if (hasHoneycombMR1()) {
            return bitmap.byteCount
        }

        return bitmap.rowBytes * bitmap.height
    }

    internal fun getDiskCacheDir(context: Context, uniqueName: String): File {
        val cachePath = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !isExternalStorageRemovable)
            getExternalCacheDir(context)?.path
        else
            context.cacheDir.path

        return File(cachePath + File.separator + uniqueName)
    }

    val isExternalStorageRemovable: Boolean
        @TargetApi(VERSION_CODES.GINGERBREAD)
        get() {
            if (hasGingerbread()) {
                return Environment.isExternalStorageRemovable()
            }
            return true
        }

    @TargetApi(VERSION_CODES.FROYO)
    internal fun getExternalCacheDir(context: Context): File? {
        if (hasFroyo()) {
            return context.externalCacheDir
        }

        val cacheDir = "/Android/data/" + context.packageName + "/cache/"
        return File(Environment.getExternalStorageDirectory().path + cacheDir)
    }

    @TargetApi(VERSION_CODES.GINGERBREAD)
    internal fun getUsableSpace(path: File): Long {
        if (hasGingerbread()) {
            return path.usableSpace
        }
        val stats = StatFs(path.path)
        return stats.blockSize.toLong() * stats.availableBlocks.toLong()
    }

    @TargetApi(VERSION_CODES.KITKAT)
    internal fun canUseForInBitmap(
            candidate: Bitmap, targetOptions: BitmapFactory.Options): Boolean {
        if (!hasKitKat()) {
            return candidate.width == targetOptions.outWidth
                    && candidate.height == targetOptions.outHeight
                    && targetOptions.inSampleSize == 1
        }
        val width = targetOptions.outWidth / targetOptions.inSampleSize
        val height = targetOptions.outHeight / targetOptions.inSampleSize
        val byteCount = width * height * getBytesPerPixel(candidate.config)
        return byteCount <= candidate.allocationByteCount
    }

    internal fun getBytesPerPixel(config: Bitmap.Config): Int {
        if (config == Bitmap.Config.ARGB_8888) {
            return 4
        } else if (config == Bitmap.Config.RGB_565) {
            return 2
        } else if (config == Bitmap.Config.ARGB_4444) {
            return 2
        } else if (config == Bitmap.Config.ALPHA_8) {
            return 1
        }
        return 1
    }

    fun hashKeyForDisk(key: String): String {
        var cacheKey: String
        try {
            val mDigest = MessageDigest.getInstance("MD5")
            mDigest.update(key.toByteArray())
            cacheKey = bytesToHexString(mDigest.digest())
        } catch (e: NoSuchAlgorithmException) {
            cacheKey = key.hashCode().toString()
        }

        return cacheKey
    }

    internal fun bytesToHexString(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (i in bytes.indices) {
            val hex = Integer.toHexString(0xFF and bytes[i].toInt())
            if (hex.length == 1) {
                sb.append('0')
            }
            sb.append(hex)
        }
        return sb.toString()
    }

    fun getDefaultImageDiskCacheDir(context: Context): File?{
        return getDiskCacheDir(context, "image")
    }

}
