# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("intrack")


# configure import libs
set(distribution_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../distribution)

add_library(lib_apriltag STATIC IMPORTED)
set_target_properties(lib_apriltag PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/apriltag/lib/${ANDROID_ABI}/libapriltag.a)


add_library(${CMAKE_PROJECT_NAME} SHARED
        acceleration-ins.cpp
        accelerationSensor/AccelerationSensorProcessor.cpp
        utils/Helpers.cpp
        apriltagprocessor.cpp)


target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
        ${distribution_DIR}/apriltag/include)

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(${CMAKE_PROJECT_NAME}
        # List libraries link to the target library
        lib_apriltag
        android
        log)