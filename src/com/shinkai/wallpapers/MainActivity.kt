package com.shinkai.wallpapers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import android.widget.FrameLayout
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
        playShapesAnimation()

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

    private fun playShapesAnimation() {
        val overlay = findViewById<FrameLayout>(R.id.shapes_overlay)
        val mainContent = findViewById<View>(R.id.main_content)
        val circle = findViewById<ImageView>(R.id.shape_circle)
        val triangle = findViewById<ImageView>(R.id.shape_triangle)
        val flower = findViewById<ImageView>(R.id.shape_flower)
        val diamond = findViewById<ImageView>(R.id.shape_diamond)

        val dp = resources.displayMetrics.density

        circle.translationY = -200f * dp
        circle.translationX = -100f * dp
        triangle.translationX = -300f * dp
        triangle.translationY = 150f * dp
        flower.translationX = 300f * dp
        flower.translationY = 100f * dp
        diamond.translationY = 400f * dp
        diamond.translationX = 50f * dp

        val duration = 900L

        val animCircle = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(circle, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(circle, "translationX", -100f * dp, 0f),
                ObjectAnimator.ofFloat(circle, "translationY", -200f * dp, 0f)
            )
            this.duration = duration
            interpolator = DecelerateInterpolator()
        }

        val animTriangle = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(triangle, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(triangle, "translationX", -300f * dp, 0f),
                ObjectAnimator.ofFloat(triangle, "translationY", 150f * dp, 0f)
            )
            this.duration = duration
            startDelay = 150
            interpolator = DecelerateInterpolator()
        }

        val animFlower = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(flower, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(flower, "translationX", 300f * dp, 0f),
                ObjectAnimator.ofFloat(flower, "translationY", 100f * dp, 0f)
            )
            this.duration = duration
            startDelay = 300
            interpolator = DecelerateInterpolator()
        }

        val animDiamond = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(diamond, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(diamond, "translationX", 50f * dp, 0f),
                ObjectAnimator.ofFloat(diamond, "translationY", 400f * dp, 0f)
            )
            this.duration = duration
            startDelay = 450
            interpolator = DecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(animCircle, animTriangle, animFlower, animDiamond)
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    mainContent.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                    overlay.animate()
                        .alpha(0f)
                        .setStartDelay(200)
                        .setDuration(400)
                        .withEndAction { overlay.visibility = View.GONE }
                        .start()
                }
            })
            start()
        }
    }

    private fun fetchWallpapersOnline() {
        lifecycleScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    NativeLib.fetchWallpapers(JSON_URL)
                }
                val arr = JSONArray(json)
                val wallpapers = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    Wallpaper(
                        o.getString("name"),
                        o.getString("thumbnail_url"),
                        o.getString("full_url")
                    )
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
