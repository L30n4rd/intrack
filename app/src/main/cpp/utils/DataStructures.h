//
// Created by LeonardPiontek on 1/26/2024.
//

#ifndef INTRACK_DATASTRUCTURES_H
#define INTRACK_DATASTRUCTURES_H

#include <stdint.h>
#include <jni.h>

struct AccelerationInsJNILookupCache {
    // ArrayList
    jclass arrayListClass;
    jmethodID arrayListConstructor;
    jmethodID arrayListAddMethod;
    // AccelerationSample
    jclass accelerationSampleClass;
    jmethodID accelerationSampleConstructor;
    // AccelerationInsJNI
    jmethodID updateVelocityMethod;
};

struct SensorEvent {
    int32_t size;
    int32_t sensorReportToken;
    int32_t type;
    uint32_t atomicCounter;
    int64_t timestamp;
    float data[16];
    int32_t reserved[4];
};

struct SensorEventSample {
    int64_t timestamp;
    float data[3];
};

#endif //INTRACK_DATASTRUCTURES_H
