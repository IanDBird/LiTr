#include <jni.h>
#include <string>

#include "Logging.h"
#include "MediaMuxer.h"

//https://github.com/aosp-mirror/platform_frameworks_base/blob/master/media/java/android/media/MediaMuxer.java
//https://sources.debian.org/src/android-framework-23/6.0.1+r72-3/frameworks/base/media/jni/android_media_MediaMuxer.cpp
//https://github.com/sztwang/TX2_libstagefright/blob/master/MediaMuxer.cpp

/**
 * Helper to throw a JNI based exception.
 *
 * @param env The JNI environment.
 * @param className The name of the Java class which represents the exception.
 * @param message The message associated with the exception.
 */
void jniThrowException(JNIEnv *env, const char* className, const char* message) {
    jclass exClass = env->FindClass(className);
    if (exClass != nullptr) {
        env->ThrowNew(exClass, message);
    }
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_linkedin_android_litr_muxers_NativeMediaMuxer_nativeSetup(
        JNIEnv *env,
        jobject /* this */,
        jstring jOutputPath,
        jstring jFormatName) {
    const char* path = env->GetStringUTFChars(jOutputPath, nullptr);
    const char* formatName = env->GetStringUTFChars(jFormatName, nullptr);

    // Initialise the MediaMuxer, using the path and format provided.
    auto muxer = new MediaMuxer(path, formatName);
    auto err = muxer->init();
    if (err == -1) {
        LOGE("Unable to initialise MediaMuxer");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Failed to initialise the muxer");
    }

    env->ReleaseStringUTFChars(jOutputPath, path);
    env->ReleaseStringUTFChars(jFormatName, formatName);

    return reinterpret_cast<jlong>(muxer);
}

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_muxers_NativeMediaMuxer_nativeStart(
        JNIEnv *env,
        jobject /* this */,
        jlong nativeObject) {
    auto* muxer = reinterpret_cast<MediaMuxer*>(nativeObject);
    if (muxer == nullptr) {
        LOGE("Muxer was not set up correctly");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Muxer was not set up correctly");
    }

    auto err = muxer->start();
    if (err == -1) {
        LOGE("Unable to start MediaMuxer");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Failed to start the muxer");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_muxers_NativeMediaMuxer_nativeStop(
        JNIEnv *env,
        jobject /* this */,
        jlong nativeObject) {
    auto* muxer = reinterpret_cast<MediaMuxer*>(nativeObject);
    if (muxer == nullptr) {
        LOGE("Muxer was not set up correctly");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Muxer was not set up correctly");
    }

    auto err = muxer->stop();
    if (err == -1) {
        LOGE("Unable to stop MediaMuxer");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Failed to stop the muxer");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_muxers_NativeMediaMuxer_nativeRelease(
        JNIEnv *env,
        jobject /* this */,
        jlong nativeObject) {
    auto* muxer = reinterpret_cast<MediaMuxer*>(nativeObject);
    delete muxer;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_linkedin_android_litr_muxers_NativeMediaMuxer_nativeAddTrack(
        JNIEnv *env,
        jobject /* this */,
        jlong nativeObject,
        jobjectArray values) {

    return -1;
}

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_muxers_NativeMediaMuxer_nativeWriteSampleData(
        JNIEnv *env,
        jobject /* this */,
        jlong nativeObject,
        jint trackIndex,
        jobject byteBuf,
        jint offset, jint size,
        jlong presentationTimeUs,
        jint flags) {
    auto* muxer = reinterpret_cast<MediaMuxer*>(nativeObject);
    if (muxer == nullptr) {
        LOGE("Muxer was not set up correctly");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Muxer was not set up correctly");
    }

    jclass byteBufClass = env->FindClass("java/nio/ByteBuffer");
    jmethodID arrayID = env->GetMethodID(byteBufClass, "array", "()[B");

    auto byteArray = (jbyteArray)env->CallObjectMethod(byteBuf, arrayID);
    if (byteArray == nullptr) {
        LOGE("byteArray is null");
        jniThrowException(env, "java/lang/IllegalArgumentException",
                          "byteArray is null");
        return;
    }

    jboolean isCopy;
    auto dst = env->GetByteArrayElements(byteArray, &isCopy);
    auto dstSize = env->GetArrayLength(byteArray);

    if (dstSize < (offset + size)) {
        LOGE("writeSampleData saw wrong dstSize %lld, size  %d, offset %d", (long long)dstSize, size, offset);
        env->ReleaseByteArrayElements(byteArray, (jbyte *)dst, 0);
        jniThrowException(env, "java/lang/IllegalArgumentException",
                          "sample has a wrong size");
        return;
    }

    // Now that we have access to the underlying buffer, let's use that to build a suitable sample
    // to write via the Muxer.
    auto err = muxer->writeSampleData(trackIndex, (uint8_t *) dst + offset, size, presentationTimeUs, flags);

    env->ReleaseByteArrayElements(byteArray, (jbyte *)dst, 0);

    if (err == -1) {
        LOGE("writeSampleData returned an error");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "writeSampleData returned an error");
    }
}