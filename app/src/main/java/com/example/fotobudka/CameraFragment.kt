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
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.fotobudka.databinding.FragmentCameraBinding
import com.example.fotobudka.db.CameraDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody

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

        binding.optionsBtn.setOnClickListener {
            navController.navigate(R.id.action_cameraFragment_to_optionsFragment)
        }

        binding.takePhotoBtn.setOnClickListener {
            val settings = db.cameraDao().readAllSettings()
            val a = settings.amount
            val d = settings.duration
            val b = settings.before

            val dd: Long = d.toLong() * 1000
            val bb: Long = b.toLong() * 1000

            makeDir()
            for (i in 1 .. a) {
                Handler(Looper.getMainLooper()).postDelayed({
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (i != a) {
                            takePhoto()
                            takePhotoSound.start()
                        } else {
                            takePhoto()
                            takePhotoSound.start()
                            Handler(Looper.getMainLooper()).postDelayed({
                                endOfSeriesSound.start()
                                makeCollage()
                            }, 2000)
                    }}, bb * i)
                }, (dd-bb) * i)
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
        val date = SimpleDateFormat(Constants.FILE_NAME_FORMAT,
            Locale.getDefault()).format(System.currentTimeMillis())
        val photoFile = File(
            outputDirectory, "$date.jpg"
        )
        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOption, ContextCompat.getMainExecutor(ctx),
            object: ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "PhotoSaved"
                    sending(photoFile, date)
                    Toast.makeText(ctx, "$msg $savedUri", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(Constants.TAG, "onError: ${exception.message}", exception)
                }

            }
        )
    }

    private fun makeDir() {
        GlobalScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            val dirName = SimpleDateFormat(Constants.FILE_NAME_FORMAT,
                Locale.getDefault()).format(System.currentTimeMillis()).toString()
            val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), "{\"dirName\":\"$dirName\"}")
            val request = Request.Builder()
                .url("http://192.168.1.143:3000/create-directory")
                .post(requestBody)
                .build()
            val response = client.newCall(request).execute()
            println(response.body?.string())
        }
    }

    private fun makeCollage() {
        GlobalScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://192.168.1.143:3000/collage")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                println(responseBody)
            } else {
                println("Error: ${response.code}")
            }
        }
    }

    private fun sending(photoFile: File, name: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "photo",
                    "$name.jpg",
                    photoFile.asRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("http://192.168.1.143:3000/upload")
                .post(requestBody)
                .build()

            client.newCall(request).execute()
        }
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