package com.detribes.timerapplication.features.timerscreen.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startForegroundService
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.detribes.timerapplication.R
import com.detribes.timerapplication.app.services.TimerService
import com.detribes.timerapplication.databinding.FragmentTimerScreenBinding
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class TimerScreenFragment : Fragment() {

    private var _binding: FragmentTimerScreenBinding? = null
    private val binding: FragmentTimerScreenBinding
        get() = _binding ?: throw RuntimeException("Fragment is null")

    private val viewModel: TimerScreenViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    val timeText = when (state) {
                        is TimerState.Idle -> formatTime(0)
                        is TimerState.Finished -> formatTime(0)
                        is TimerState.Running -> formatTime(state.timeLeft)
                        is TimerState.Paused -> formatTime(state.timeLeft)
                    }

                    val startPauseText = getString(state.startPauseText)
                    val resetVisibility = state.visibility

                    binding.textViewCountdown.text = timeText
                    binding.startPauseBtn.text = startPauseText
                    binding.resetBtn.visibility = resetVisibility
                }
            }
        }

    }

    private fun setupListeners() {
        binding.setTimeBtn.setOnClickListener {
            val seconds = binding.editTextInput.text.toString().toLongOrNull() ?: 0L
            val intent = Intent(requireContext(), TimerService::class.java)
                .putExtra("duration", seconds)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(requireContext(), intent)
            } else {
                requireContext().startService(intent)
            }
            viewModel.startTimer(seconds)
        }

        binding.startPauseBtn.setOnClickListener {
            val seconds = binding.textViewCountdown.text.split(":").map { it.toInt() }
                .let { it[0] * 3600 + it[1] * 60 + it[2] }

            when (viewModel.state.value) {
                is TimerState.Running -> viewModel.pauseTimer()
                is TimerState.Paused -> viewModel.resumeTimer()
                is TimerState.Idle -> viewModel.startTimer(seconds.toLong())
                else -> Unit
            }
        }

        binding.resetBtn.setOnClickListener {
            viewModel.stopTimer()
        }
    }

    private fun formatTime(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(getString(R.string.formated_string), hours, minutes, seconds)
    }

    companion object {
        @JvmStatic
        fun newInstance() = TimerScreenFragment()
    }
}
