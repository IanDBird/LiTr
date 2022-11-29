//
// Created by Ian on 29/11/2022.
//

#include "MediaMuxer.h"
#include "FFmpeg.h"
#include "Logging.h"

// https://github.com/FFmpeg/FFmpeg/blob/n3.0/doc/examples/muxing.c#L569

MediaMuxer::MediaMuxer(const char *path, const char *formatName)
    : mPath(path),
      mFormatName(formatName) {

}

int MediaMuxer::init() {
    int err = avformat_alloc_output_context2(&mContext, nullptr, mFormatName, mPath);
    if (err < 0) {
        LOGE("Failed to allocate AVFormatContext");
        return err;
    }

    return 0;
}

int MediaMuxer::start() {
    int err;

    // Open File.
    err = avio_open(&mContext->pb, mPath, AVIO_FLAG_WRITE);
    if (err < 0) {
        LOGE("Failed to open file");
        return err;
    }

    // Write the stream header, if any.
    AVDictionary *opt; // TODO
    err = avformat_write_header(mContext, &opt);
    if (err < 0) {
        LOGE("Failed to write stream header");
        return err;
    }

    return 0;
}

int MediaMuxer::stop() {
    // Write the trailer, if any.
    int err = av_write_trailer(mContext);
    if (err < 0) {
        LOGE("Failed to write trailer");
        return err;
    }

    // Close output file.
    avio_closep(&mContext->pb);

    // Free the context.
    avformat_free_context(mContext);
    return 0;
}

int MediaMuxer::addVideoStream(const char *codec_name, int64_t bitrate, int width, int height,
                               int frameRate, uint8_t *extradata, int extradata_size) {
    AVStream* stream = addStream(codec_name, bitrate, extradata, extradata_size);
    if (!stream) {
        LOGE("Failed to create new stream");
        return -1;
    }

    // Add the video specific stream details.
    stream->codecpar->width = width;
    stream->codecpar->height = height;
    stream->time_base = (AVRational){ 1, frameRate };

    return stream->index;
}

int MediaMuxer::addAudioStream(const char *codec_name, int64_t bitrate, int channels,
                               int sample_rate, uint8_t *extradata, int extradata_size) {
    AVStream* stream = addStream(codec_name, bitrate, extradata, extradata_size);
    if (!stream) {
        LOGE("Failed to create new stream");
        return -1;
    }

    // Add the audio specific stream details.
    stream->codecpar->channels = channels;
    stream->codecpar->sample_rate = sample_rate;
    stream->time_base = (AVRational){ 1,sample_rate };

    return stream->index;
}

AVStream* MediaMuxer::addStream(const char *codec_name, int64_t bitrate, uint8_t *extradata, int extradata_size) {
    AVStream* stream = avformat_new_stream(mContext, nullptr);
    if (!stream) {
        LOGE("Failed to allocate new stream");
        return nullptr;
    }

    // Look up the AVCodecDescriptor based upon it's name. If we don't locate/understand it, we are
    // unable to determine the codec type (video, audio, etc) as well as it's AVCodecID.
    const AVCodecDescriptor* descriptor = avcodec_descriptor_get_by_name(codec_name);
    if (!descriptor) {
        LOGE("Failed to identify AVCodecDescriptor by name");
        return nullptr;
    }

    // Build known, common, stream details...
    stream->codecpar->codec_type = descriptor->type;
    stream->codecpar->codec_id = descriptor->id;
    stream->codecpar->bit_rate = bitrate;
    stream->codecpar->extradata = extradata;
    stream->codecpar->extradata_size = extradata_size;

    return stream;
}

int MediaMuxer::writeSampleData(int stream_index, uint8_t *buffer, int size, int64_t pts, int flags) {
    // Build the packet that we will attempt to write.
    AVPacket pkt = { 0 }; // Data and size must be 0;
    av_init_packet(&pkt);

    // Populate the packet data with what we've been given. Since we're using the buffer directly
    // we will not wrap it in a AVBuffer/Ref instance.
    pkt.stream_index = stream_index;
    pkt.data = buffer;
    pkt.size = size;
    pkt.pts = pts;
    pkt.flags = flags;

    // Write the compressed frame to the media file.
    int err = av_interleaved_write_frame(mContext, &pkt);
    if (err < 0) {
        LOGE("Failed to write frame");
        return -1;
    }

    return 0;
}