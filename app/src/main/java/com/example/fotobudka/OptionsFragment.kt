package com.example.fotobudka

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.fotobudka.databinding.FragmentOptionsBinding
import com.example.fotobudka.db.CameraDatabase

class OptionsFragment : Fragment() {

    lateinit var navController: NavController
    private var _binding: FragmentOptionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var ctx: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOptionsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        val db = CameraDatabase.getInstance(ctx)

        binding.submit.setOnClickListener {
            val numberOfPhotos = binding.numberOfPhotosET.text.toString()
            val duration = binding.breakDurationET.text.toString()
            val beforePhoto = binding.timeBeforePhotoET.text.toString()

            if (numberOfPhotos.isEmpty() || duration.isEmpty() || beforePhoto.isEmpty()) {
                Toast.makeText(ctx, "Number of photos, duration and time before first" +
                        " must be > 2", Toast.LENGTH_SHORT).show()
            }
            else if (numberOfPhotos.toInt() < 2 || duration.toInt() < 2 || beforePhoto.toInt() < 2) {
                Toast.makeText(ctx, "Number of photos, duration and time before first" +
                        " must be > 2", Toast.LENGTH_SHORT).show()
            } else if (beforePhoto > duration) {
                Toast.makeText(ctx, "Duration cannot be shorter than time of the sound" +
                        "before photo", Toast.LENGTH_SHORT).show()
            } else {
                db.cameraDao().updateSettings(numberOfPhotos.toInt(), duration.toInt(), beforePhoto.toInt())

                navController.navigate(R.id.action_optionsFragment_to_cameraFragment)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        ctx = context
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}