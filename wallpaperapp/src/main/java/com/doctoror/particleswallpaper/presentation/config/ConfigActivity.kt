/*
 * Copyright (C) 2017 Yaroslav Mytkalyk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.doctoror.particleswallpaper.presentation.config

import android.annotation.TargetApi
import android.app.ActionBar
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.annotation.ColorInt
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import android.widget.Toolbar
import com.doctoror.particlesdrawable.ParticlesDrawable
import com.doctoror.particleswallpaper.R
import com.doctoror.particleswallpaper.data.config.DrawableConfiguratorFactory
import com.doctoror.particleswallpaper.data.repository.SettingsRepositoryFactory
import com.doctoror.particleswallpaper.domain.config.DrawableConfigurator
import com.doctoror.particleswallpaper.domain.interactor.SetWallpaperUseCase
import com.doctoror.particleswallpaper.domain.repository.SettingsRepository
import com.doctoror.particleswallpaper.presentation.compat.ViewCompat
import com.doctoror.particleswallpaper.presentation.util.ThemeUtils
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction

class ConfigActivity : Activity() {

    private val requestCodeSetWallpaper = 1;

    private val particlesDrawable = ParticlesDrawable()

    private val settings: SettingsRepository by lazy {
        SettingsRepositoryFactory.provide(this)
    }

    private val configurator: DrawableConfigurator by lazy {
        DrawableConfiguratorFactory.provideDrawableConfigurator()
    }

    private var bgDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)
        ViewCompat.setBackground(findViewById(R.id.drawableContainer), particlesDrawable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ConfigActivityLollipop.onCreate(this)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_config, menu)
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }

            R.id.actionPreview -> {
                SetWallpaperUseCase(this, requestCodeSetWallpaper).useCase().subscribe({
                    v ->
                    if (!v) Toast.makeText(this, R.string.Failed_to_start_preview, Toast.LENGTH_LONG).show()
                })
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        bgDisposable = Observable.combineLatest(
                settings.getBackgroundUri(),
                settings.getBackgroundColor(),
                BiFunction<String, Int, Pair<String, Int>> { t1, t2 -> Pair(t1!!, t2!!) })
                .subscribe({ result: Pair<String, Int> -> applyBackground(result) })
        configurator.subscribe(particlesDrawable, settings)
        particlesDrawable.start()
    }

    override fun onStop() {
        super.onStop()
        bgDisposable?.dispose()
        particlesDrawable.stop()
        configurator.dispose()
    }

    private fun applyBackground(result: Pair<String, Int>) {
        applyBackground(result.first, result.second)
    }

    private fun applyBackground(uri: String, @ColorInt color: Int) {
        val bg = findViewById(R.id.bg) as ImageView
        if (uri == "") {
            onNoBackgroundImage(bg, color)
        } else {
            Picasso.with(this)
                    .load(uri)
                    .into(bg, object : Callback {
                        override fun onSuccess() {
                            ViewCompat.setBackground(bg, null)
                        }

                        override fun onError() {
                            onNoBackgroundImage(bg, color)
                        }
                    })
        }
    }

    private fun onNoBackgroundImage(bg: ImageView, @ColorInt color: Int) {
        bg.setImageDrawable(null)
        ViewCompat.setBackground(bg,
                (if (color == Color.BLACK) null else ColorDrawable(color)))
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class ConfigActivityLollipop {
        companion object {
            @JvmStatic fun onCreate(a: ConfigActivity) {
                val root = a.findViewById(R.id.toolbarContainer) as ViewGroup
                val toolbar = Toolbar(a)
                root.addView(toolbar, 0,
                        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ThemeUtils.getDimension(a.theme, android.R.attr.actionBarSize).toInt()))
                a.setActionBar(toolbar)
                a.actionBar?.displayOptions = ActionBar.DISPLAY_HOME_AS_UP or ActionBar.DISPLAY_SHOW_HOME
            }
        }
    }
}