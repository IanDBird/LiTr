package com.linkedin.android.litr.muxers

import android.media.MediaCodec.BufferInfo
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer

class NativeMediaMuxer(val path: String, val format: String) {
    private var nativeObject: Long = 0L
    private var state: Int = MUXER_STATE_UNINITIALIZED

    private var lastTrackIndex = -1

    init {
        // Initialise the native muxer.
        nativeObject = nativeSetup(path, format)
        state = MUXER_STATE_INITIALIZED
    }

    /**
     * Adds a track with the specified format.
     */
    fun addTrack(format: MediaFormat): Int {
        if (state != MUXER_STATE_INITIALIZED) {
            throw IllegalStateException("Muxer is not initialized.")
        }
        if (nativeObject == 0L) {
            throw IllegalStateException("Muxer has been released")
        }

        // TODO: Actually add tracks
        val values = format.toStreamValues()
        Log.d(TAG, values.joinToString(", "))
        val trackIndex = nativeAddTrack(nativeObject, values)

        // The returned track index is expected to be incremented as addTrack succeeds. However, if
        // the format is invalid, it will get a negative track index.
        if (lastTrackIndex >= trackIndex) {
            throw IllegalStateException("Invalid format")
        }

        lastTrackIndex = trackIndex
        return trackIndex
    }

    /**
     * Starts the muxer.
     */
    fun start() {
        if (nativeObject == 0L) {
            throw IllegalStateException("Muxer has been released")
        }

        if (state == MUXER_STATE_INITIALIZED) {
            nativeStart(nativeObject)
            state = MUXER_STATE_STARTED
        } else {
            throw IllegalStateException(
                    "Can't start due to wrong state (${convertMuxerStateCodeToString(state)})")
        }
    }

    /**
     * Stops the muxer.
     */
    fun stop() {
        if (state == MUXER_STATE_STARTED) {
            try {
                nativeStop(nativeObject);
            } catch (e: Exception) {
                throw e
            } finally {
                state = MUXER_STATE_STOPPED
            }
        } else {
            throw IllegalStateException(
                    "Can't stop due to wrong state (${convertMuxerStateCodeToString(state)})")
        }
    }

    /**
     * Writes an encoded sample into the muxer.
     */
    fun writeSampleData(trackIndex: Int, byteBuf: ByteBuffer, bufferInfo: BufferInfo) {
        if (trackIndex < 0 || trackIndex > lastTrackIndex) {
            throw IllegalArgumentException("trackIndex is invalid")
        }
        if (bufferInfo.size < 0 || bufferInfo.offset < 0 ||
                (bufferInfo.offset + bufferInfo.size) > byteBuf.capacity()) {
            throw IllegalArgumentException("bufferInfo must specify a valid buffer offset and size")
        }
        if (nativeObject == 0L) {
            throw IllegalStateException("Muxer has been released")
        }
        if (state != MUXER_STATE_STARTED) {
            throw IllegalStateException("Can't write, muxer is not started")
        }

        // Pass the buffer and info to the native layer.
        nativeWriteSampleData(
                nativeObject,
                trackIndex,
                byteBuf,
                bufferInfo.offset,
                bufferInfo.size,
                bufferInfo.presentationTimeUs,
                bufferInfo.flags
        )
    }

    /**
     * Make sure you call this when you're done to free up any resources, instead of relying on the
     * garbage collector to do this for you at some point in the future.
     */
    fun release() {
        if (state == MUXER_STATE_STARTED) {
            stop()
        }

        if (nativeObject != 0L) {
            nativeRelease(nativeObject)
            nativeObject = 0L
        }

        state = MUXER_STATE_UNINITIALIZED
    }

    protected fun finalize() {
        if (nativeObject != 0L) {
            nativeRelease(nativeObject)
            nativeObject = 0L
        }
    }

    private external fun nativeSetup(outputPath: String, formatName: String) : Long
    private external fun nativeRelease(nativeObject: Long)
    private external fun nativeStart(nativeObject: Long)
    private external fun nativeStop(nativeObject: Long)
    private external fun nativeAddTrack(nativeObject: Long, values: Array<String>): Int
    private external fun nativeWriteSampleData(nativeObject: Long, trackIndex: Int,
                                               byteBuf: ByteBuffer, offset: Int, size: Int,
                                               presentationTimeUs: Long, flags: Int)

    companion object {
        init {
            // Ensure that our native libraries are loaded.
            NativeMuxersLib.loadLibraries()
        }

        val TAG = NativeMediaMuxer::class.simpleName

        const val MUXER_STATE_UNINITIALIZED = -1
        const val MUXER_STATE_INITIALIZED = 0
        const val MUXER_STATE_STARTED = 1
        const val MUXER_STATE_STOPPED = 2

        private fun convertMuxerStateCodeToString(state: Int): String {
            return when(state) {
                MUXER_STATE_UNINITIALIZED -> { "UNINITIALIZED" }
                MUXER_STATE_INITIALIZED -> { "INITIALIZED" }
                MUXER_STATE_STARTED -> { "STARTED" }
                MUXER_STATE_STOPPED -> { "STOPPED " }
                else -> { "UNKNOWN" }
            }
        }
    }
}