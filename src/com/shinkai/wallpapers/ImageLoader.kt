package com.shinkai.wallpapers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

object ImageLoader {

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    private val executor = Executors.newFixedThreadPool(4)
    private val handler = Handler(Looper.getMainLooper())

    fun load(context: Context, imageUrl: String, imageView: ImageView, targetWidth: Int = 0, targetHeight: Int = 0) {
        imageView.tag = imageUrl

        val cachedBitmap = memoryCache.get(imageUrl)
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap)
            return
        }

        imageView.setImageDrawable(null)
        val viewRef = WeakReference(imageView)

        executor.execute {
            try {
                val cacheDir = context.cacheDir.absolutePath
                val localPath = NativeLib.downloadImage(imageUrl, cacheDir)
                val localFile = File(localPath)

                val bitmap = decodeSampledBitmapFromFile(localFile.absolutePath, targetWidth, targetHeight)

                if (bitmap != null) {
                    memoryCache.put(imageUrl, bitmap)

                    handler.post {
                        val view = viewRef.get()
                        if (view != null && view.tag == imageUrl) {
                            view.setImageBitmap(bitmap)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun decodeSampledBitmapFromFile(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        if (reqWidth <= 0 || reqHeight <= 0) {
            return BitmapFactory.decodeFile(path)
        }

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
