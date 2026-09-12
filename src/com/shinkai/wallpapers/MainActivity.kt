package com.shinkai.wallpapers

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

class MainActivity : AppCompatActivity() {
    
    private var allWallpapers: List<Wallpaper> = listOf()
    private lateinit var grid: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private val JSON_URL = "https://raw.githubusercontent.com/ShinkaiProject/shinkai-walls-assets/heptakaideka/wallpapers.json"

    override fun onCreate(savedInstanceState: Bundle?) {

        DynamicColors.applyToActivityIfAvailable(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        grid = findViewById(R.id.wallpaper_grid)
        grid.layoutManager = GridLayoutManager(this, 2)
        
        // Optimasi RecyclerView untuk scroll super halus
        grid.setHasFixedSize(true)
        grid.setItemViewCacheSize(20)

        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeColors(
            MaterialColors.getColor(swipeRefresh, com.google.android.material.R.attr.colorPrimary)
        )
        swipeRefresh.setProgressBackgroundColorSchemeColor(
            MaterialColors.getColor(swipeRefresh, com.google.android.material.R.attr.colorSurface)
        )
        swipeRefresh.setOnRefreshListener {
            fetchWallpapersOnline()
        }

        fetchWallpapersOnline()

        findViewById<ImageButton>(R.id.btn_about).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_title)
                .setIcon(R.mipmap.ic_launcher)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.about_dismiss, null)
                .show()
        }

        findViewById<View>(R.id.fab_search).setOnClickListener {
            val input = EditText(this).apply {
                hint = getString(R.string.search_hint)
                setPadding(48, 32, 48, 32)
                background = null
            }

            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.search_title)
                .setView(input)
                .setPositiveButton(R.string.searchPositiveButton) { _, _ ->
                    val keyword = input.text.toString().trim().lowercase()
                    
                    val filteredList = if (keyword.isEmpty()) {
                        allWallpapers
                    } else {
                        allWallpapers.filter { it.name.lowercase().contains(keyword) }
                    }
                    
                    if (filteredList.isEmpty()) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.search_not_found_title)
                            .setMessage(R.string.search_not_found_message)
                            .setPositiveButton(R.string.search_not_found_dismiss, null)
                            .show()
                    } else {
                        setupAdapter(grid, filteredList)
                    }
                }
                .setNegativeButton(R.string.searchNegativeButton) { _, _ ->
                    setupAdapter(grid, allWallpapers)
                }
                .show()
        }
    }

    private fun fetchWallpapersOnline() {
        lifecycleScope.launch {
            try {
                val wallpapers = withContext(Dispatchers.IO) {
                    val json = URL(JSON_URL).readText()
                    val arr = JSONArray(json)
                    (0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        
                        val name = o.getString("name")
                        val imageUrl = o.getString("thumbnail_url")
                        val fullUrl = o.getString("full_url")
                        
                        Wallpaper(name, imageUrl, fullUrl)
                    }
                }
                allWallpapers = wallpapers
                setupAdapter(grid, allWallpapers)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, R.string.error_network, Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun setupAdapter(grid: RecyclerView, list: List<Wallpaper>) {
        grid.adapter = WallpaperAdapter(list) { wp ->
            startActivity(Intent(this, PreviewActivity::class.java)
                .putExtra("asset_path", wp.fullUrl)
                .putExtra("wallpaper_name", wp.name))
        }
    }
}
