// app/src/main/cpp/native-lib.cpp

#include <jni.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp> // For cvtColor and Canny
#include <vector>

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_android_1opencv_1gl_1assignment_MainActivity_processFrame(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray yuv_data,
        jint width,
        jint height) {

    // Get a pointer to the raw YUV data
    jbyte *yuv_bytes = env->GetByteArrayElements(yuv_data, JNI_FALSE);
    cv::Mat mat_yuv(height + height / 2, width, CV_8UC1, yuv_bytes);

    // --- Start of Image Processing Pipeline ---

    // 1. Convert YUV to Grayscale for Canny detection.
    //    This is more efficient than converting to RGBA first.
    cv::Mat mat_gray(height, width, CV_8UC1);
    cv::cvtColor(mat_yuv, mat_gray, cv::COLOR_YUV2GRAY_NV21);

    // 2. Apply Canny Edge Detection.
    //    The two numbers (50, 150) are the min and max thresholds.
    //    You can experiment with these values.
    cv::Mat mat_edges(height, width, CV_8UC1);
    cv::Canny(mat_gray, mat_edges, 50, 150);

    // 3. Convert the single-channel edge map back to a 4-channel RGBA image.
    //    This is necessary so the output data format is consistent and can be
    //    correctly interpreted by our OpenGL texture renderer.
    cv::Mat mat_display(height, width, CV_8UC4);
    cv::cvtColor(mat_edges, mat_display, cv::COLOR_GRAY2RGBA);

    // --- End of Image Processing Pipeline ---

    // Convert the final display Mat back into a jbyteArray to return to Kotlin.
    size_t num_bytes = mat_display.total() * mat_display.elemSize();
    jbyteArray output_byte_array = env->NewByteArray(num_bytes);
    env->SetByteArrayRegion(output_byte_array, 0, num_bytes, (jbyte *)mat_display.data);

    // Clean up
    env->ReleaseByteArrayElements(yuv_data, yuv_bytes, 0);

    return output_byte_array;
}