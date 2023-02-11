package com.example.fotobudka

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.fotobudka.databinding.FragmentOptionsBinding

class OptionsFragment : Fragment() {

    lateinit var navController: NavController
    private var _binding: FragmentOptionsBinding? = null
    private val binding get() = _binding!!

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

        binding.submit.setOnClickListener {
            val numberOfPhotos = binding.numberOfPhotosET.text.toString()
            val clicked = true.toString()
            val bundle = bundleOf("amount" to numberOfPhotos, "clicked" to clicked)

            navController.navigate(R.id.action_optionsFragment_to_cameraFragment, bundle)
        }
    }
}