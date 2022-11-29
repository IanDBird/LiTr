/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr;

import androidx.annotation.Nullable;

public class MimeType {
    private static final String AUDIO_PREFIX = "audio/";

    public static final String AUDIO_AAC = "audio/mp4a-latm";
    public static final String AUDIO_RAW = "audio/raw";
    public static final String AUDIO_OPUS = "audio/opus";
    public static final String AUDIO_VORBIS = "audio/vorbis";

    private static final String VIDEO_PREFIX = "video/";

    public static final String VIDEO_AVC = "video/avc";
    public static final String VIDEO_HEVC = "video/hevc";
    public static final String VIDEO_VP8 = "video/x-vnd.on2.vp8";
    public static final String VIDEO_VP9 = "video/x-vnd.on2.vp9";

    public static boolean IsVideo(@Nullable String mimeType) {
        return mimeType != null && mimeType.startsWith(VIDEO_PREFIX);
    }

    public static boolean IsAudio(@Nullable String mimeType) {
        return mimeType != null && mimeType.startsWith(AUDIO_PREFIX);
    }
}
