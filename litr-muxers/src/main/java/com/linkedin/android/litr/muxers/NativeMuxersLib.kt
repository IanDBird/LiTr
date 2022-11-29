package com.linkedin.android.litr.muxers

import android.util.Log

class NativeMuxersLib {
    companion object {
        private val TAG = NativeMuxersLib::class.simpleName
        private val FFMPEG_LIBRARIES = listOf("avutil", "swresample", "avcodec", "avformat", "litr-muxers")

        @JvmStatic
        fun loadLibraries() {
            FFMPEG_LIBRARIES.forEach {
                loadLibrary(it)
            }
        }

        private fun loadLibrary(libraryName: String) {
            try {
                System.loadLibrary(libraryName)
                Log.i(TAG, "Loaded: lib$libraryName")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Unable to load: lib$libraryName")
            }
        }
    }
}