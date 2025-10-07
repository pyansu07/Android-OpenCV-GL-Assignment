package com.example.android_opencv_gl_assignment

// app/src/main/java/com/yourpackage/MyGLRenderer.kt

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer : GLSurfaceView.Renderer {

    // --- Shader Code ---
    // Standard vertex shader to draw a textured rectangle
    private val vertexShaderCode = """
        attribute vec4 a_Position;
        attribute vec2 a_TexCoordinate;
        varying vec2 v_TexCoordinate;
        void main() {
            v_TexCoordinate = a_TexCoordinate;
            gl_Position = a_Position;
        }
    """.trimIndent()

    // Standard fragment shader to sample from the texture
    private val fragmentShaderCode = """
        precision mediump float;
        uniform sampler2D u_Texture;
        varying vec2 v_TexCoordinate;
        void main() {
            gl_FragColor = texture2D(u_Texture, v_TexCoordinate);
        }
    """.trimIndent()

    // --- Buffers and Handles ---
    private val vertexBuffer: FloatBuffer
    private val textureBuffer: FloatBuffer
    private var shaderProgram: Int = 0
    private var textureId: Int = 0

    // --- Frame Data ---
    @Volatile private var frameData: ByteBuffer? = null
    @Volatile private var frameWidth: Int = 0
    @Volatile private var frameHeight: Int = 0
    private var isFrameNew = false

    init {
        // A rectangle that fills the screen
        val vertices = floatArrayOf(
            -1.0f, -1.0f, // Bottom-left
            1.0f, -1.0f,  // Bottom-right
            -1.0f, 1.0f,  // Top-left
            1.0f, 1.0f   // Top-right
        )
        // Mapping the texture coordinates to the vertices
        val textureCoords = floatArrayOf(
            0.0f, 1.0f, // Bottom-left
            1.0f, 1.0f, // Bottom-right
            0.0f, 0.0f, // Top-left
            1.0f, 0.0f  // Top-right
        )

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices)
        vertexBuffer.position(0)

        textureBuffer = ByteBuffer.allocateDirect(textureCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(textureCoords)
        textureBuffer.position(0)
    }

    // Public method for MainActivity to update the frame data
    fun updateFrame(data: ByteArray, width: Int, height: Int) {
        frameData = ByteBuffer.wrap(data)
        frameWidth = width
        frameHeight = height
        isFrameNew = true
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Compile shaders and link program
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        shaderProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
        // Generate texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        // Set texture parameters
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    override fun onDrawFrame(gl: GL10?) {
        // If a new frame has arrived from the camera, upload it to the texture
        if (isFrameNew && frameData != null && frameWidth > 0 && frameHeight > 0) {
            frameData!!.position(0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                frameWidth, frameHeight, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, frameData
            )
            isFrameNew = false
        }

        // --- Drawing Logic ---
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(shaderProgram)

        // Get handles to shader attributes and uniforms
        val positionHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Position")
        val texCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "a_TexCoordinate")
        val textureHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Texture")

        // Pass in the texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureHandle, 0)

        // Pass in vertex positions
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        // Pass in texture coordinates
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)

        // Draw the rectangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}