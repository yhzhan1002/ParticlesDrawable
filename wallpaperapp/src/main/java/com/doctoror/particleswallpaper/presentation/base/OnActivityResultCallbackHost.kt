package com.doctoror.particleswallpaper.presentation.base

/**
 * Created by Yaroslav Mytkalyk on 31.05.17.
 */
interface OnActivityResultCallbackHost {

    fun registerCallback(callback : OnActivityResultCallback)
    fun unregsiterCallback(callback: OnActivityResultCallback)
}