# Real-Time Edge Detection Viewer

This project is an Android application that captures a live camera feed, processes it in real-time using OpenCV in C++ to perform edge detection, and renders the output using OpenGL ES. It also includes a minimal TypeScript web viewer to display a sample processed frame.

## [cite_start]Features Implemented [cite: 65]

### Android Application
- [x] Live camera feed integration using CameraX.
- [x] JNI bridge to pass camera frames to native C++ code.
- [x] Canny Edge Detection implemented in C++ using OpenCV.
- [x] Real-time rendering of the processed frames using OpenGL ES 2.0.
- [x] (Optional) Add any bonus features you implemented here, like an FPS counter.

### Web Viewer
- [x] Minimal web page built with TypeScript and HTML.
- [x] Displays a static sample frame processed by the Android app.
- [x] Shows an overlay with basic frame statistics (resolution, FPS).

## [cite_start]Demo [cite: 67]

*(It's highly recommended to record a short GIF of your app working and place it here.)*

![App Demo](link_to_your_gif_or_screenshot.gif)

## [cite_start]Architecture Overview [cite: 69]

The application follows a simple data flow from camera capture to screen rendering:

1.  **Camera (Kotlin):** The `CameraX` library provides a stream of frames in `YUV_420_888` format.
2.  **JNI Bridge:** Each frame is converted to an `NV21` byte array and passed from Kotlin to the native C++ layer.
3.  **Processing (C++):** The C++ code receives the byte array, wraps it in an OpenCV `cv::Mat`, performs a `YUV -> Grayscale -> Canny Edge` conversion, and finally converts the result to an RGBA `Mat`.
4.  **JNI Bridge:** The final RGBA data is returned to Kotlin as a byte array.
5.  **OpenGL (Kotlin/GLSL):** The byte array is uploaded to an OpenGL texture in the `MyGLRenderer` and drawn onto a `GLSurfaceView` for display.

The web component is a separate, static page that demonstrates basic TypeScript usage by displaying a Base64-encoded sample frame.

## [cite_start]Setup and Build Instructions [cite: 68]

1.  **Prerequisites:**
    * Android Studio (latest version recommended).
    * Android NDK (version **23.1.7779620** is specified in the build script).
    * OpenCV Android SDK (version **4.9.0** was used).

2.  **Configuration:**
    * Clone the repository.
    * Download the **OpenCV 4.9.0 Android SDK** and extract it.
    * Open `app/src/main/cpp/CMakeLists.txt` and update the `OpenCV_DIR` path to point to the `sdk/native` directory of your extracted OpenCV SDK.
    * Open the project in Android Studio. It should sync and build automatically.

3.  **Web Viewer:**
    * Navigate to the `/web` directory.
    * Run `npm install` (if you added any packages) and then `tsc` to compile the TypeScript.
    * Open `index.html` in a web browser.