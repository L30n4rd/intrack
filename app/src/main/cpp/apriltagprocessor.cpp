#include <jni.h>
#include <string>
#include <iterator>
#include "nlohmann/json.hpp"

#include <apriltag.h>
#include <apriltag_pose.h>
#include <tagStandard41h12.h>

using json = nlohmann::json;


// Function to mirror an image along the y-axis
image_u8_t* mirrorImageYAxis(const image_u8_t* inputImage) {
    unsigned int width = inputImage->width;
    unsigned int height = inputImage->height;

    // Create a new image to store the mirrored image
    image_u8_t* mirroredImage = image_u8_copy(inputImage);

    // Mirror the image along the y-axis
    for (unsigned int y = 0; y < height; ++y) {
        for (unsigned int x = 0; x < width; ++x) {
            // Copy pixel values from input image to mirrored image, but flip horizontally
            mirroredImage->buf[y * width + x] = inputImage->buf[(height - y - 1) * width + x];
        }
    }

    return mirroredImage;
}

// Function declaration
json apriltag_detection_to_json(const apriltag_detection_t* det,
                                const apriltag_pose_t* pose,
                                double estimate_error);

extern "C" JNIEXPORT jstring JNICALL
Java_com_l30n4rd_intrack_data_apriltag_AprilTagProcessorJNI_detectTags(
        JNIEnv* env,
        jclass clazz,
        jobject imageBuffer,
        jint width,
        jint height,
        jdouble tagsizeInMeters,
        jdouble focalLengthInPixelsX,
        jdouble focalLengthInPixelsY,
        jdouble focalCenterInPixelsX,
        jdouble focalCenterInPixelsY) {

    std::string tags;

    uint8_t* imageData = reinterpret_cast<uint8_t*>(env->GetDirectBufferAddress(imageBuffer));
    image_u8_t cameraImage = {
            .width = width,
            .height = height,
            .stride = width,
            .buf = imageData
    };

    image_u8_t* im = &cameraImage;

    apriltag_detector_t *td = apriltag_detector_create();
    td->nthreads = 8;
//    td->quad_decimate = 2.0;

    apriltag_family_t *tf = tagStandard41h12_create();
    apriltag_detector_add_family(td, tf);

    zarray_t *detections = apriltag_detector_detect(td, im);
    nlohmann::json detections_json = json::array();

    for (int i = 0; i < zarray_size(detections); i++) {
        apriltag_detection_t *det;
        zarray_get(detections, i, &det);

        // Pose Estimation
        apriltag_detection_info_t tagInfo = {
                .det =  det,
                .tagsize =  tagsizeInMeters,
                .fx =  focalLengthInPixelsX,
                .fy =  focalLengthInPixelsY,
                .cx =  focalCenterInPixelsX,
                .cy =  focalCenterInPixelsY
        };
        apriltag_pose_t pose;
        double err = estimate_tag_pose(&tagInfo, &pose);

        // Add detection to json array
        detections_json.push_back(apriltag_detection_to_json(det, &pose, err));
    }

    tags = detections_json.dump();

    // Cleanup
    apriltag_detections_destroy(detections);
    tagStandard41h12_destroy(tf);
    apriltag_detector_destroy(td);

    return env->NewStringUTF(tags.c_str());
}

json apriltag_detection_to_json(const apriltag_detection_t* det,
                                const apriltag_pose_t* pose,
                                double estimate_error) {

    // Convert 'data' array to a vector before adding it to the JSON object
    std::vector<double> poseTArray(pose->t->data, pose->t->data + pose->t->nrows * pose->t->ncols);
    std::vector<double> poseRArray(pose->R->data, pose->R->data + pose->R->nrows * pose->R->ncols);

    json detection_json = {
        {"id", det->id},
        {"c", det->c},
        {"p", det->p},
        {"err", estimate_error},
        {"pose", {
            {"t", poseTArray},
            {"r", poseRArray}
        }}
    };

    return detection_json;
}
