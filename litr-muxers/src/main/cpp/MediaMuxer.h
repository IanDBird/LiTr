//
// Created by Ian on 29/11/2022.
//

#ifndef LITR_MEDIAMUXER_H
#define LITR_MEDIAMUXER_H

#include "FFmpeg.h"

class MediaMuxer
{
public:
    MediaMuxer(const char *path, const char *formatName);
    int init();
    int start();
    int stop();
    int addVideoStream(const char * codec_name, int64_t bitrate, int width, int height,
                       int frame_rate, uint8_t *extradata, int extradata_size);
    int addAudioStream(const char * codec_name, int64_t bitrate, int channels, int sample_rate,
                       uint8_t *extradata, int extradata_size);
    int writeSampleData(int stream_index, uint8_t *buffer, int size, int64_t pts, int flags);

private:
    const char* mPath;
    const char* mFormatName;

    AVFormatContext *mContext = nullptr;

    AVStream* addStream(const char * codec_name, int64_t bitrate, uint8_t *extradata, int extradata_size);
};

#endif //LITR_MEDIAMUXER_H
