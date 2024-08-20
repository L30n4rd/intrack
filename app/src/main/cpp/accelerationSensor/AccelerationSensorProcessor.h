//
// Created by LeonardPiontek on 1/26/2024.
//

#ifndef INTRACK_ACCELERATIONSENSORPROCESSOR_H
#define INTRACK_ACCELERATIONSENSORPROCESSOR_H

#include <mutex>
#include <vector>
#include <android/hardware_buffer_jni.h>

#include "../utils/DataStructures.h"

class AccelerationSensorProcessor {
public:
    static constexpr double NS2S = 1.0e-9f;

    AccelerationSensorProcessor();
    ~AccelerationSensorProcessor();

    // Processing
    bool processAccelerationData(JNIEnv *env, jobject *thiz, jobject hardwareBuffer);
    void stopProcessing();

    // Recording
    void startRecording();
    void stopRecording();
    jobject *getRecordedData(JNIEnv *env);

private:
    std::mutex mutex;
    bool processingFlag;
    bool recordingFlag;
    std::vector<SensorEventSample> recordedData;
    float velocity[3];

    void applyHighPassFilter(
            SensorEvent &event,
            float *gravity,
            float *linearAcceleration,
            float alpha);
};


#endif //INTRACK_ACCELERATIONSENSORPROCESSOR_H
