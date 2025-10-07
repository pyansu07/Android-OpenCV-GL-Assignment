package com.example.android_opencv_gl_assignment

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: MyGLRenderer
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        glSurfaceView = findViewById(R.id.glSurfaceView)

        // Configure GLSurfaceView
        glSurfaceView.setEGLContextClientVersion(2)
        renderer = MyGLRenderer()
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Request permission
        checkCameraPermission()
    }

// In MainActivity.kt

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission has been granted, now we can safely start the camera.
            Log.d("MainActivity", "Permission granted, starting camera.")
            startCamera()
        } else {
            // The user denied the permission.
            Log.e("MainActivity", "Camera permission denied.")
            // You could show a message to the user here.
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // If permission is already granted, start the camera directly.
                Log.d("MainActivity", "Permission already granted, starting camera.")
                startCamera()
            }
            else -> {
                // Otherwise, launch the permission request dialog.
                // The result will be handled by the launcher above.
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(
                cameraExecutor,
                ImageAnalyzer(renderer, glSurfaceView, this::processFrame)
            )

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("MainActivity", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private class ImageAnalyzer(
        private val renderer: MyGLRenderer,
        private val glSurfaceView: GLSurfaceView,
        private val processFrame: (ByteArray, Int, Int) -> ByteArray
    ) : ImageAnalysis.Analyzer {

        override fun analyze(image: ImageProxy) {
            val nv21 = yuv420ToNv21(image)

            // Call native function
            val processedData = processFrame(nv21, image.width, image.height)
            Log.d("FrameData", android.util.Base64.encodeToString(processedData, android.util.Base64.DEFAULT))

            // Pass to OpenGL renderer
            renderer.updateFrame(processedData, image.width, image.height)

            // Request redraw
            glSurfaceView.requestRender()

            image.close()
        }

        /** ✅ Safe YUV_420_888 → NV21 conversion */
        private fun yuv420ToNv21(image: ImageProxy): ByteArray {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            // Copy Y plane
            yBuffer.get(nv21, 0, ySize)

            // Interleave U and V for NV21 format
            val chromaRowStride = image.planes[1].rowStride
            val chromaPixelStride = image.planes[1].pixelStride

            var offset = ySize
            val width = image.width
            val height = image.height

            val uBytes = ByteArray(uSize)
            val vBytes = ByteArray(vSize)
            uBuffer.get(uBytes)
            vBuffer.get(vBytes)

            for (row in 0 until height / 2) {
                var uvRowStart = row * chromaRowStride
                for (col in 0 until width / 2) {
                    val u = uBytes[uvRowStart + col * chromaPixelStride]
                    val v = vBytes[uvRowStart + col * chromaPixelStride]

                    nv21[offset++] = v
                    nv21[offset++] = u
                }
            }

            return nv21
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    /**
     * Native function for frame processing (C++)
     */
    external fun processFrame(yuvData: ByteArray, width: Int, height: Int): ByteArray

    companion object {
        init {
            // 1. Load the C++ shared library first, which OpenCV depends on.
            System.loadLibrary("c++_shared")

            // 2. Load the main OpenCV library.
            System.loadLibrary("opencv_java4")

            // 3. Load your own native library last.
            System.loadLibrary("android_opencv_gl_assignment")
        }
    }
}
