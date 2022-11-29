package com.linkedin.android.litr.muxers

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import com.linkedin.android.litr.exception.MediaTargetException
import com.linkedin.android.litr.io.MediaTarget
import com.linkedin.android.litr.io.MediaTargetSample
import java.nio.ByteBuffer
import java.util.*

class NativeMediaMuxerMediaTarget(
        private val outputFilePath: String,
        private val trackCount: Int,
        private val orientationHint: Int,
        outputFormat: String
): MediaTarget {
    private val queue: LinkedList<MediaTargetSample> = LinkedList()
    private var isStarted: Boolean = false
    private val mediaMuxer : NativeMediaMuxer

    private var numberOfTracksToAdd = 0
    private val mediaFormatsToAdd = arrayOfNulls<MediaFormat>(trackCount)

    init {
        try {
            mediaMuxer = NativeMediaMuxer(outputFilePath, outputFormat)
        } catch (ex: IllegalStateException) {
            throw MediaTargetException(
                    MediaTargetException.Error.INVALID_PARAMS,
                    outputFilePath,
                    outputFormat,
                    ex);
        }
    }

    override fun addTrack(mediaFormat: MediaFormat, targetTrack: Int): Int {
        mediaFormatsToAdd[targetTrack] = mediaFormat
        numberOfTracksToAdd++

        if (numberOfTracksToAdd == trackCount) {
            Log.d(TAG, "All tracks added, starting MediaMuxer, writing out ${queue.size} queued samples")

            mediaFormatsToAdd.filterNotNull().forEach {
                mediaMuxer.addTrack(it)
            }

            mediaMuxer.start()
            isStarted = true

            // Write out any queued samples.
            while (queue.isNotEmpty()) {
                val sample = queue.removeFirst()
                mediaMuxer.writeSampleData(sample.targetTrack, sample.buffer, sample.info)
            }
        }

        return targetTrack
    }

    override fun writeSampleData(targetTrack: Int, buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (isStarted) {
            mediaMuxer.writeSampleData(targetTrack, buffer, info)
        } else {
            // The NativeMediaMuxer is not yet started, so queue up incoming buffers to write them out later
            queue.addLast(MediaTargetSample(targetTrack, buffer, info))
        }
    }

    override fun release() {
        mediaMuxer.release()
    }

    override fun getOutputFilePath() = outputFilePath

    companion object {
        private val TAG = NativeMediaMuxerMediaTarget::class.simpleName
    }
}