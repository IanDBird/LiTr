package com.linkedin.android.litr.muxers

import android.media.MediaFormat
import com.linkedin.android.litr.MimeType

const val KEY_MIME_TYPE = MediaFormat.KEY_MIME
const val KEY_BIT_RATE = MediaFormat.KEY_BIT_RATE
const val KEY_PROFILE = "profile" // MediaFormat.KEY_PROFILE
const val KEY_LEVEL = "level" // MediaFormat.KEY_LEVEL
const val KEY_WIDTH = MediaFormat.KEY_WIDTH
const val KEY_HEIGHT = MediaFormat.KEY_HEIGHT
const val KEY_CHANNEL_COUNT = MediaFormat.KEY_CHANNEL_COUNT
const val KEY_SAMPLE_RATE = MediaFormat.KEY_SAMPLE_RATE

fun MediaFormat.toStreamValues(): Array<String> {
    val values = mutableListOf<String>()

    // Codec ID
    values.add(getCodecId())

    // Bitrate / Profile / Level
    values.add(getStringSafe(KEY_BIT_RATE, 0))
    values.add(getStringSafe(KEY_PROFILE, 0))
    values.add(getStringSafe(KEY_LEVEL, 0))

    // Add codec type specific metadata.
    val mimeType = getStringSafe(KEY_MIME_TYPE, "")
    if (MimeType.IsVideo(mimeType)) {
        // Width / Height
        values.add(getStringSafe(KEY_WIDTH, 0))
        values.add(getStringSafe(KEY_HEIGHT, 0))
    } else if (MimeType.IsAudio(mimeType)) {
        // Channel Count / Sample Rate
        values.add(getStringSafe(KEY_CHANNEL_COUNT, 0))
        values.add(getStringSafe(KEY_SAMPLE_RATE, 0))
    }

    return values.toTypedArray()
}

private fun MediaFormat.getCodecId(): String {
    val mimeType = getStringSafe(KEY_MIME_TYPE, "")
    val components = mimeType.split("/")
    return components.last()
}

fun MediaFormat.getStringSafe(key: String, default: Int): String {
    return runCatching { getInteger(key) }.getOrDefault(default).toString()
}

fun MediaFormat.getStringSafe(key: String, default: String): String {
    return getString(key) ?: default
}