package com.example.fotobudka

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.fotobudka.databinding.FragmentCameraBinding
import com.example.fotobudka.db.CameraDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private lateinit var navController: NavController
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var activity: Activity
    private lateinit var externalMediaDirs: Array<File>
    private lateinit var ctx: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = requireActivity()
        ctx = context
        externalMediaDirs = context.externalMediaDirs
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        if (allPermissionGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                activity, Constants.REQUIRED_PERMISSIONS, Constants.REQUEST_CODE_PERMISSIONS
            )
        }

        val db = CameraDatabase.getInstance(ctx)
        if (db.cameraDao().ifSettings() == 0) {
            val settings = com.example.fotobudka.db.entity.Camera(5, 5, 3)
            db.cameraDao().addSettings(settings)
        }

        val takePhotoSound = MediaPlayer.create(ctx, R.raw.make_photo)
        val endOfSeriesSound = MediaPlayer.create(ctx, R.raw.end_of_series)
        val beforePhotoSound = MediaPlayer.create(ctx, R.raw.before_photo)

        fun betweenPhotos() {
            takePhoto()
            takePhotoSound.start()
        }

        fun lastPhoto() {
            takePhoto()
            takePhotoSound.start()
            Handler(Looper.getMainLooper()).postDelayed({
                endOfSeriesSound.start()
            }, 2000)
        }

        binding.optionsBtn.setOnClickListener {
            navController.navigate(R.id.action_cameraFragment_to_optionsFragment)
        }

        binding.takePhotoBtn.setOnClickListener {
            val settings = db.cameraDao().readAllSettings()
            val a = settings.amount
            val d = settings.duration
            val b = settings.before

            val dd: Long = d.toLong() * 1000
            var bb: Long = b.toLong() * 1000




            val handler = Handler()
            val numRuns = b
            var counter = 0

            val runnable = object : Runnable {
                override fun run() {
                    if (counter < numRuns) {
                            beforePhotoSound.start()
                        counter++
                        handler.postDelayed(this, 1000)
                    } else {
                        handler.removeCallbacks(this)
                    }
                }
            }




            for (i in 0 until a) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (i != a - 1) {
                        betweenPhotos()
                    } else {
                        lastPhoto()
                    }}, dd * i)
            }
        }
    }

    private fun getOutputDirectory() : File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
                mFile -> File(mFile, resources.getString(R.string.app_name)).apply {
            mkdirs()
            }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else activity.filesDir
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory, SimpleDateFormat(Constants.FILE_NAME_FORMAT,
                Locale.getDefault()).format(System.currentTimeMillis()) + ".jpg")
        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOption, ContextCompat.getMainExecutor(ctx),
            object: ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "PhotoSaved"
                    Toast.makeText(ctx, "$msg $savedUri", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(Constants.TAG, "onError: ${exception.message}", exception)
                }

            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionGranted()) {
                startCamera()
            } else {
                Toast.makeText(ctx, "Permissions not granted", Toast.LENGTH_SHORT).show()
                activity.finish()
            }
        }
    }

    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                activity.baseContext, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun startCamera() {
        val cameraProviderFeature = ProcessCameraProvider.getInstance(ctx)

        cameraProviderFeature.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFeature.get()
            val preview = Preview.Builder()
                .build().also {
                        mPreview -> mPreview.setSurfaceProvider(
                    binding.cameraPV.surfaceProvider
                )
                }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: java.lang.Exception) {
                Log.d(Constants.TAG, "startCamera Fail: ", e)
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        _binding = null
    }
}