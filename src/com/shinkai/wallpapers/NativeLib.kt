package com.shinkai.wallpapers

import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

object NativeLib {

    val available: Boolean

    init {
        available = try {
            System.loadLibrary("shinkai")
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        }
    }

    @Throws(Exception::class)
    external fun fetchWallpapersNative(url: String): String

    @Throws(Exception::class)
    external fun downloadImageNative(url: String, cacheDir: String): String

    @Throws(Exception::class)
    external fun hashKeyNative(key: String): String

    fun fetchWallpapers(url: String): String {
        if (available) return fetchWallpapersNative(url)
        return fetchWallpapersKotlin(url)
    }

    fun downloadImage(url: String, cacheDir: String): String {
        if (available) return downloadImageNative(url, cacheDir)
        return downloadImageKotlin(url, cacheDir)
    }

    private fun fetchWallpapersKotlin(url: String): String {
        val conn = URL(url).openConnection().apply {
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        return conn.getInputStream().bufferedReader().use { it.readText() }
    }

    private fun downloadImageKotlin(url: String, cacheDir: String): String {
        val fileName = hashKeyKotlin(url)
        val cachePath = File(cacheDir, "image_cache").apply { mkdirs() }
        val file = File(cachePath, fileName)

        if (file.exists()) return file.absolutePath

        val tmpFile = File(cachePath, "$fileName.tmp")
        val conn = URL(url).openConnection().apply {
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        conn.getInputStream().use { input ->
            FileOutputStream(tmpFile).use { output ->
                input.copyTo(output)
            }
        }
        tmpFile.renameTo(file)
        return file.absolutePath
    }

    private fun hashKeyKotlin(key: String): String {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(key.toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
