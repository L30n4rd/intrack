cmake_minimum_required(VERSION 3.22.1)

project("intrack")

set(CMAKE_VERBOSE_MAKEFILE on)

set(lib_src_DIR ${CMAKE_CURRENT_SOURCE_DIR})

set(lib_build_DIR $ENV{HOME}/tmp/${ANDROID_ABI})
file(MAKE_DIRECTORY ${lib_build_DIR})

# AprilTag
add_subdirectory(${lib_src_DIR}/apriltag ${lib_build_DIR}/apriltag)
