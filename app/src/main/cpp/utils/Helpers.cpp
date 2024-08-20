//
// Created by LeonardPiontek on 1/26/2024.
//

#include <android/log.h>
#include "Helpers.h"

void updateIdLookupCache(JNIEnv *env, AccelerationInsJNILookupCache *cache) {
    // Find the ArrayList class
    cache->arrayListClass = env->FindClass("java/util/ArrayList");
    if (cache->arrayListClass == nullptr) {
        // Handle error
        __android_log_print(ANDROID_LOG_ERROR, "AccelerationInsJNI", "Error: ArrayListClass is null");
        return;
    }

    // Get the ArrayList constructor
    cache->arrayListConstructor = env->GetMethodID(cache->arrayListClass, "<init>", "()V");
    if (cache->arrayListConstructor == nullptr) {
        // Handle error
        __android_log_print(ANDROID_LOG_ERROR, "AccelerationInsJNI", "Error: ArrayListConstructor is null");
        return;
    }

    // Get the ArrayList add method ID
    cache->arrayListAddMethod = env->GetMethodID(cache->arrayListClass, "add", "(Ljava/lang/Object;)Z");
    if (cache->arrayListAddMethod == nullptr) {
        // Handle error
        __android_log_print(ANDROID_LOG_ERROR, "AccelerationInsJNI", "Error: ArrayListAddMethod is null");
        return;
    }

    // Find the AccelerationSample class
    cache->accelerationSampleClass = env->FindClass("com/l30n4rd/intrack/model/AccelerationSample");
    if (cache->accelerationSampleClass == nullptr) {
        // Handle error
        __android_log_print(ANDROID_LOG_ERROR, "AccelerationInsJNI", "Error: AccelerationSampleClass is null");
        return;
    }

    // Get the AccelerationSample constructor method ID
    cache->accelerationSampleConstructor = env->GetMethodID(cache->accelerationSampleClass, "<init>", "(J[F)V");
    if (cache->accelerationSampleConstructor == nullptr) {
        // Handle error
        __android_log_print(ANDROID_LOG_ERROR, "AccelerationInsJNI", "Error: AccelerationSampleConstructor is null");
        return;
    }

    // Find the AccelerationInsJNI class
    jclass accelerationInsJNIClass = env->FindClass("com/l30n4rd/intrack/data/ins/impl/AccelerationInsJNI");
    if (accelerationInsJNIClass == nullptr) {
        // Handle error
        __android_log_print(ANDROID_LOG_ERROR, "AccelerationInsJNI", "Error: AccelerationInsJNIClass is null");
        return;
    }

    // Get the updateVelocity method ID
    cache->updateVelocityMethod = env->GetMethodID(accelerationInsJNIClass, "updateVelocity", "(FFF)V");
    if (cache->updateVelocityMethod == nullptr) {
        // Handle error
        __android_log_print(ANDROID_LOG_ERROR, "AccelerationInsJNI", "Error: updateVelocityMethod is null");
        return;
    }
}
