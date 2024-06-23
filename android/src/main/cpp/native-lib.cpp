#include <oboe/Oboe.h>
#include <jni.h>
#include <android/log.h>
#include <vector>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "CustomMediaRecorder", __VA_ARGS__)

class MyOboeCallback : public oboe::AudioStreamCallback {
public:
    std::vector<int16_t> audioBuffer;

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override {
        int16_t *intData = static_cast<int16_t*>(audioData);
        audioBuffer.insert(audioBuffer.end(), intData, intData + numFrames * audioStream->getChannelCount());
        return oboe::DataCallbackResult::Continue;
    }
};

static MyOboeCallback *callback = nullptr;
static oboe::AudioStream *stream = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_tchvu3_capacitorvoicerecorder_CustomMediaRecorder_startOboeRecording(JNIEnv *env, jobject obj, jint deviceId) {
    oboe::AudioStreamBuilder builder;
    callback = new MyOboeCallback();

    builder.setDirection(oboe::Direction::Input)
           .setPerformanceMode(oboe::PerformanceMode::LowLatency)
           .setSharingMode(oboe::SharingMode::Exclusive)
           .setFormat(oboe::AudioFormat::I16)
           .setChannelCount(oboe::ChannelCount::Stereo)
           .setSampleRate(44100)
           .setDeviceId(deviceId)
           .setCallback(callback);

    oboe::Result result = builder.openStream(&stream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open stream. Error: %s", oboe::convertToText(result));
        return JNI_FALSE;
    }

    result = stream->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start stream. Error: %s", oboe::convertToText(result));
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

extern "C" JNIEXPORT jshortArray JNICALL
Java_com_tchvu3_capacitorvoicerecorder_CustomMediaRecorder_getOboeRecordedData(JNIEnv *env, jobject obj) {
    if (callback == nullptr) {
        return nullptr;
    }

    jshortArray result = env->NewShortArray(callback->audioBuffer.size());
    env->SetShortArrayRegion(result, 0, callback->audioBuffer.size(), reinterpret_cast<jshort*>(callback->audioBuffer.data()));
    
    callback->audioBuffer.clear();
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_tchvu3_capacitorvoicerecorder_CustomMediaRecorder_stopOboeRecording(JNIEnv *env, jobject obj) {
    if (stream != nullptr) {
        stream->requestStop();
        stream->close();
        delete stream;
        stream = nullptr;
    }

    if (callback != nullptr) {
        delete callback;
        callback = nullptr;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_tchvu3_capacitorvoicerecorder_CustomMediaRecorder_pauseOboeRecording(JNIEnv *env, jobject obj) {
    if (stream != nullptr) {
        oboe::Result result = stream->requestPause();
        return result == oboe::Result::OK ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_tchvu3_capacitorvoicerecorder_CustomMediaRecorder_resumeOboeRecording(JNIEnv *env, jobject obj) {
    if (stream != nullptr) {
        oboe::Result result = stream->requestStart();
        return result == oboe::Result::OK ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}