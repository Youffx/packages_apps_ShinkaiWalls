package com.shinkai.wallpapers

import android.app.WallpaperManager
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class PreviewActivity : AppCompatActivity() {
    
    private var wallpaperName: String = "wallpaper"

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        // Menerima URL dan Nama Wallpaper dari Intent
        val imageUrl = intent.getStringExtra("asset_path") ?: return finish()
        wallpaperName = intent.getStringExtra("wallpaper_name") ?: "wallpaper"
        
        val imageView = findViewById<ImageView>(R.id.preview_image)

        // Dapatkan resolusi layar untuk memuat preview secara optimal
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Load gambar preview dengan ImageLoader yang teroptimasi
        ImageLoader.load(
            context = this,
            imageUrl = imageUrl,
            imageView = imageView,
            targetWidth = screenWidth,
            targetHeight = screenHeight
        )

        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btn_next).setOnClickListener {
            showApplyBottomSheet(imageUrl)
        }
    }

    private fun showApplyBottomSheet(url: String) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_apply, null)
        dialog.setContentView(view)

        val cbLock = view.findViewById<CheckBox>(R.id.cb_lockscreen)
        val cbHome = view.findViewById<CheckBox>(R.id.cb_homescreen)
        val btnApply = view.findViewById<MaterialButton>(R.id.btn_apply)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnApply.setOnClickListener {
            var flag = 0
            if (cbLock.isChecked) flag = flag or WallpaperManager.FLAG_LOCK
            if (cbHome.isChecked) flag = flag or WallpaperManager.FLAG_SYSTEM

            if (flag == 0) {
                Toast.makeText(this, R.string.preview_select_screen, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialog.dismiss()
            setWallpaperWithLoading(url, wallpaperName, flag)
        }

        dialog.show()
    }

    private fun setWallpaperWithLoading(urlString: String, name: String, flag: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)
        
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            val safeFileName = "${name.replace(Regex("[^A-Za-z0-9]"), "_")}.jpg"
            val wallsDirectory = File(filesDir, "saved_wallpapers")
            val localFile = File(wallsDirectory, safeFileName)

            try {
                if (!wallsDirectory.exists()) {
                    wallsDirectory.mkdirs()
                }

                if (!localFile.exists()) {
                    val connection = URL(urlString).openConnection()
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 10_000
                    
                    connection.getInputStream().use { input ->
                        FileOutputStream(localFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val wallpaperManager = WallpaperManager.getInstance(this@PreviewActivity)
                localFile.inputStream().use { stream ->
                    wallpaperManager.setStream(stream, null, true, flag)
                }

                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) {
                        loadingDialog.dismiss()
                        Toast.makeText(this@PreviewActivity, R.string.preview_wallpaper_set, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                
                if (localFile.exists() && localFile.length() == 0L) {
                    localFile.delete()
                }

                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) {
                        loadingDialog.dismiss()
                        Toast.makeText(this@PreviewActivity, getString(R.string.preview_wallpaper_failed, e.message), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
