cmake_minimum_required(VERSION 3.22.1)

project("android_opencv_gl_assignment")

# --- Manual OpenCV Configuration ---

# 1. Set the path to your OpenCV SDK's native directory
set(OpenCV_DIR "C:/Users/pyans/Downloads/opencv-4.12.0-android-sdk/OpenCV-android-sdk/sdk/native")

# 2. Add the OpenCV shared library (.so file) as a prebuilt dependency
add_library(lib_opencv SHARED IMPORTED)
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION
        ${OpenCV_DIR}/libs/${ANDROID_ABI}/libopencv_java4.so)

# 3. Point to the OpenCV header files for compilation
include_directories(${OpenCV_DIR}/jni/include)

# --- Your Project's Library Definition ---

add_library(
        android_opencv_gl_assignment
        SHARED
        native-lib.cpp)

find_library(
        log-lib
        log)

# --- Linking ---

# Link your library to the log library and our prebuilt OpenCV library
target_link_libraries(
        android_opencv_gl_assignment
        lib_opencv
        ${log-lib})