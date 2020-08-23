package com.example.novitaguok.android_opencv

import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.android_opencv.R
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DetectionActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private var TAG = "MainActivityOpenCV"

    private lateinit var mRGBA : Mat
    private lateinit var javaCameraView : CameraBridgeViewBase
    private lateinit var cascadeClassifier: CascadeClassifier
    private lateinit var mCascadeFile : File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_detection)

        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV is Configured or Connected Successfully")
        } else {
            Log.d(TAG, "OpenCV is not working or Loaded")
        }

        javaCameraView = findViewById(R.id.opencv_camera)
        javaCameraView.visibility = SurfaceView.VISIBLE
        javaCameraView.setCvCameraViewListener(this@DetectionActivity)
    }

    private var baseLoaderCallback = object : BaseLoaderCallback(this@DetectionActivity) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    try {
                        val inputStream = resources.openRawResource(R.raw.haarcascade_frontalface_alt2)
                        val cascadeDir = getDir("cascade", Context.MODE_PRIVATE)
                        mCascadeFile = File(cascadeDir, "haarcascade_frontalface_alt2.xml")
                        val os = FileOutputStream(mCascadeFile)

                        val buffer = ByteArray(4096)
                        var bytesRead : Int?

                        do {
                            bytesRead = inputStream.read(buffer)
                            if (bytesRead == -1) break
                            os.write(buffer, 0, bytesRead)
                        } while (true)

                        inputStream.close()
                        os.close()

                        cascadeClassifier = CascadeClassifier(mCascadeFile.absolutePath)
                        if (cascadeClassifier.empty()) {
                            Log.d(TAG, "Failed to load cascade classifier")
                        } else
                            Log.d(TAG,"Loaded cascade classifier from " + mCascadeFile.absolutePath)

                        cascadeDir.delete()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.e(TAG, "Failed to load cascade. Exception thrown: $e")
                    }

                    javaCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT)
                    javaCameraView.enableView()
                }
                else ->
                    super.onManagerConnected(status)
            }
        }
    }


    override fun onCameraViewStarted(width: Int, height: Int) {
        mRGBA = Mat()
    }

    override fun onCameraViewStopped() {
        mRGBA.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        mRGBA = inputFrame.rgba()

        Core.rotate(mRGBA , mRGBA , Core.ROTATE_90_COUNTERCLOCKWISE)

        // Detect faces
        val detectedFaces = MatOfRect()
        cascadeClassifier.detectMultiScale(mRGBA, detectedFaces)

        // Draw rectangle for detected faces
        for (rect : Rect in detectedFaces.toArray()) {
            Imgproc.rectangle(mRGBA,
                Point(rect.x.toDouble(), rect.y.toDouble()),
                Point(rect.x.toDouble() + rect.width, rect.y.toDouble() + rect.height),
                Scalar(0.0, 0.0, 255.0))
        }

        Core.rotate(mRGBA , mRGBA , Core.ROTATE_90_CLOCKWISE)

        // Flip image to get mirror effect
        val orientation = javaCameraView.getScreenOrientation()
        if (javaCameraView.isEmulator)
        // Treat emulators as a special case
            Core.flip(mRGBA, mRGBA, 1) // Flip along y-axis
        else {
            when (orientation) {
                // RGB image
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT ->
                    Core.flip(mRGBA, mRGBA,0) // Flip along x-axis
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE ->
                    Core.flip(mRGBA, mRGBA, 1) // Flip along y-axis
            }
        }

        return mRGBA
    }

    override fun onDestroy() {
        super.onDestroy()

        if (javaCameraView != null) {
            javaCameraView.disableView()
        }
    }

    override fun onPause() {
        super.onPause()
        if (javaCameraView != null) {
            javaCameraView.disableView()
        }
    }

    override fun onResume() {
        super.onResume()
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV is Configured or Connected Successfully")
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        } else {
            Log.d(TAG, "OpenCV is not working or Loaded")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback)
        }
    }

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}