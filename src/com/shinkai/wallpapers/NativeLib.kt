package com.shinkai.wallpapers

object NativeLib {

    init {
        System.loadLibrary("shinkai")
    }

    @Throws(Exception::class)
    external fun fetchWallpapers(url: String): String

    @Throws(Exception::class)
    external fun downloadImage(url: String, cacheDir: String): String

    @Throws(Exception::class)
    external fun hashKey(key: String): String
}
