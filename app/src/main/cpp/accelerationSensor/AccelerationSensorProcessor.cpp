//
// Created by LeonardPiontek on 1/26/2024.
//

#include <android/hardware_buffer_jni.h>
#include <android/log.h>
#include <thread>
#include "AccelerationSensorProcessor.h"
#include "../utils/Helpers.h"

AccelerationSensorProcessor::AccelerationSensorProcessor() :
        velocity{0.0f, 0.0f, 0.0f},
        processingFlag(false),
        recordingFlag(false) {}

AccelerationSensorProcessor::~AccelerationSensorProcessor() {}

void AccelerationSensorProcessor::stopProcessing() {
    {
        std::lock_guard<std::mutex> lock(mutex);
        processingFlag = false;
        recordingFlag = false;
    }
}

bool AccelerationSensorProcessor::processAccelerationData(JNIEnv *env,  jobject *thiz, jobject hardwareBuffer) {
    AHardwareBuffer *aHardwareBuffer = AHardwareBuffer_fromHardwareBuffer(env, hardwareBuffer);
    if (aHardwareBuffer == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "AccelerationInsJNI", "Error: AHardwareBuffer is null");
        return false;
    }

    float gravity[3] = {0.0f, 0.0f, 0.0f};
    float linearAcceleration[3] = {0.0f, 0.0f, 0.0f};
    float prevLinearAcceleration[3] = {0.0f, 0.0f, 0.0f};
    int64_t prevTimestamp = -1;
    uint32_t lastProcessedCounter = 0;
    double dT;

    float tau = 0.5f; // Example time constant
    float deltaT = 1.0f / 789.0f; // Previously measured sampling interval
//    float alpha = tau / (tau + deltaT);
    float alpha = 0.98;

    int convergenceIterations = 100;

    // Define a static local structure to hold cached IDs
    static AccelerationInsJNILookupCache cachedIDs;
    updateIdLookupCache(env, &cachedIDs);

    // Set the processing flag to true
    {
        std::lock_guard<std::mutex> lock(mutex);
        processingFlag = true;
    }

    while (true) {
        // Lock the AHardwareBuffer to access its data
        void* bufferData = nullptr;
        int status = AHardwareBuffer_lock(
                aHardwareBuffer,
                AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN,
                -1,
                nullptr,
                &bufferData);

        if (status != 0) {
            // Handle error: Unable to lock AHardwareBuffer
            __android_log_print(ANDROID_LOG_ERROR, "AccelerationInsJNI", "Error: Unable to lock AHardwareBuffer");
            break; // Break the loop and stop processing
        }

        // Copy the sensor event structure from the buffer
        SensorEvent sensorEvent;
        std::memcpy(&sensorEvent, bufferData, sizeof(SensorEvent));

        // Unlock the AHardwareBuffer
        AHardwareBuffer_unlock(aHardwareBuffer, nullptr);

        // Lock the mutex before modifying shared data
        {
            std::lock_guard<std::mutex> lock(mutex);

            // Check if processing should continue
            if (!processingFlag) {
                // Break the loop and stop processing
                break;
            }

            // Check if a new sensor event is available
            if (sensorEvent.atomicCounter == lastProcessedCounter) {
                // Nothing to do
                continue;
            }
            // Update the last processed atomicCounter
            lastProcessedCounter = sensorEvent.atomicCounter;

            // Save last linear acceleration for the integration
            std::copy(
                    linearAcceleration,
                    linearAcceleration + 3,
                    prevLinearAcceleration);

            // Remove the gravity contribution with the high-pass filter
            applyHighPassFilter(sensorEvent, gravity, linearAcceleration, alpha);

            // We omit the first `convergenceIterations` iterations to let the high-pass filter converge
            if (convergenceIterations >= 0) {
                convergenceIterations--;
                continue;
            }

            // Calculate the time difference to the last sensor event in seconds
            if (prevTimestamp != -1) {
                dT = (sensorEvent.timestamp - prevTimestamp) * NS2S;
            } else {
                // Set dT to 0 if the prevTimestamp is not available to ensure correct integration
                dT = 0.0;
            }

            // Integrate the acceleration to velocity
            for (int i = 0; i < 3; ++i) {
                velocity[i] += (prevLinearAcceleration[i] + linearAcceleration[i]) / 2.0f * dT;
            }

           /* This is an expensive operation and drops the update rate of this while loop
              from about 800 Hz to ~400 Hz */
           // Propagate the velocity to AccelerationInsJNI
           env->CallVoidMethod(
                   *thiz,
                   cachedIDs.updateVelocityMethod,
                   velocity[0], velocity[1], velocity[2]);

            // Check if recording is enabled
            if (recordingFlag) {
                // Create sensorEventSample
                SensorEventSample sensorEventSample;
                sensorEventSample.timestamp = sensorEvent.timestamp;
                std::copy(
                        velocity,
                        velocity + 3,
                        sensorEventSample.data);

                // Add the sample to the recorded data
                recordedData.push_back(sensorEventSample);
            }

            prevTimestamp = sensorEvent.timestamp;
        }
    }

    return true;
}

inline void AccelerationSensorProcessor::applyHighPassFilter(
        SensorEvent& event,
        float gravity[3],
        float linearAcceleration[3],
        float alpha)
{
    // Isolate the force of gravity with the low-pass filter.
    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.data[0];
    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.data[1];
    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.data[2];

    // Remove the gravity contribution with the high-pass filter.
    linearAcceleration[0] = event.data[0] - gravity[0];
    linearAcceleration[1] = event.data[1] - gravity[1];
    linearAcceleration[2] = event.data[2] - gravity[2];
}

void AccelerationSensorProcessor::startRecording() {
    {
        std::lock_guard<std::mutex> lock(mutex);
        recordingFlag = true;
    }
}

void AccelerationSensorProcessor::stopRecording() {
    {
        std::lock_guard<std::mutex> lock(mutex);
        recordingFlag = false;
    }
}

jobject * AccelerationSensorProcessor::getRecordedData(JNIEnv *env) {
    // Define a static local structure to hold cached IDs
    static AccelerationInsJNILookupCache cachedIDs;
    updateIdLookupCache(env, &cachedIDs);

    // Lock the mutex before accessing shared data
    std::lock_guard<std::mutex> lock(mutex);

    // Create a new ArrayList instance
    jobject list = env->NewObject(cachedIDs.arrayListClass, cachedIDs.arrayListConstructor);
    if (list == nullptr) {
        // Handle error
        __android_log_print(ANDROID_LOG_ERROR, "AccelerationInsJNI", "Error: ArrayListObject is null");
        return nullptr;
    }

    // Add each recorded data sample to the list
    for (const auto& sample : recordedData) {
        // Create a new AccelerationSample instance
        jfloatArray dataArray = env->NewFloatArray(3);
        env->SetFloatArrayRegion(dataArray, 0, 3, sample.data);
        jobject accelerationSampleObject = env->NewObject(
                cachedIDs.accelerationSampleClass,
                cachedIDs.accelerationSampleConstructor,
                sample.timestamp,
                dataArray);
        env->DeleteLocalRef(dataArray);
        if (accelerationSampleObject == nullptr) {
            // Handle error
            __android_log_print(ANDROID_LOG_ERROR, "AccelerationInsJNI", "Error: AccelerationSampleObject is null");
            return nullptr;
        }

        // Add the AccelerationSample object to the List
        jboolean result = env->CallBooleanMethod(list, cachedIDs.arrayListAddMethod, accelerationSampleObject);
        if (!result) {
            // Handle error
            __android_log_print(ANDROID_LOG_ERROR, "AccelerationInsJNI", "Error: Could not add AccelerationSampleObject to list");
            return nullptr;
        }
    }

    // Clear the recorded data vector
    recordedData.clear();

    return reinterpret_cast<jobject *>(list);
}
