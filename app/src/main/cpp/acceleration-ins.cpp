#include <jni.h>
#include <cstdint>
#include <cstring>
#include <android/hardware_buffer_jni.h>
#include <android/log.h>

#include "accelerationSensor/AccelerationSensorProcessor.h"


//
// Created by Leonard Piontek on 1/23/2024.
//

AccelerationSensorProcessor processor;

extern "C"
JNIEXPORT void JNICALL
Java_com_l30n4rd_intrack_data_ins_impl_AccelerationInsJNI_processAccelerationData(
        JNIEnv *env,
        jobject thiz,
        jobject hardwareBuffer)
{
    processor.processAccelerationData(env, &thiz, hardwareBuffer);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_l30n4rd_intrack_data_ins_impl_AccelerationInsJNI_stopAccelerationProcessing(
        JNIEnv *env,
        jclass clazz)
{
    processor.stopProcessing();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_l30n4rd_intrack_data_ins_impl_AccelerationInsJNI_startRecording(
        JNIEnv *env,
        jclass clazz)
{
    processor.startRecording();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_l30n4rd_intrack_data_ins_impl_AccelerationInsJNI_stopRecording(
        JNIEnv *env,
        jclass clazz)
{
    processor.stopRecording();
}

extern "C"
JNIEXPORT jobject * JNICALL
Java_com_l30n4rd_intrack_data_ins_impl_AccelerationInsJNI_getRecordedData(
        JNIEnv *env,
        jclass calzz)
{
    return processor.getRecordedData(env);
}
