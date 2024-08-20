#
# Copyright (C) 2016 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Build a static lib and copy it into distribution place for independent app to use
# we could not delete the bin directory is because android studio would error out
# when it checks target's existance. Only distribution place is used as import to app

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)

# library
aux_source_directory(src/common COMMON_SRC)
set(APRILTAG_SRCS src/apriltag.c src/apriltag_pose.c src/apriltag_quad_thresh.c)
file(GLOB TAG_FILES ${CMAKE_CURRENT_SOURCE_DIR}/src/tag*.c)
add_library(apriltag STATIC ${APRILTAG_SRCS} ${COMMON_SRC} ${TAG_FILES})

# copy out the lib binary... need to leave the static lib around to pass gradle check
set(distribution_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../distribution)
set_target_properties(apriltag
                      PROPERTIES
                      ARCHIVE_OUTPUT_DIRECTORY
                      "${distribution_DIR}/apriltag/lib/${ANDROID_ABI}")

# copy out lib header files...
set(APRILTAG_HEADERS
        ${CMAKE_CURRENT_SOURCE_DIR}/src/apriltag.h
        ${CMAKE_CURRENT_SOURCE_DIR}/src/apriltag_pose.h)
file(GLOB TAG_HEADERS ${CMAKE_CURRENT_SOURCE_DIR}/src/tag*.h)
file(GLOB COMMON_HEADERS ${CMAKE_CURRENT_SOURCE_DIR}/src/common/*.h)

file(COPY ${APRILTAG_HEADERS} ${TAG_HEADERS} DESTINATION ${distribution_DIR}/apriltag/include)
file(COPY ${COMMON_HEADERS} DESTINATION ${distribution_DIR}/apriltag/include/common)
