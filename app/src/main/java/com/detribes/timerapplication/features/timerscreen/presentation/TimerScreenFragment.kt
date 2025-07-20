package com.detribes.timerapplication.features.timerscreen.presentation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.detribes.timerapplication.app.services.TimerService
import com.detribes.timerapplication.databinding.FragmentTimerScreenBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class TimerScreenFragment : Fragment() {
    private var _binding: FragmentTimerScreenBinding? = null
    private val binding: FragmentTimerScreenBinding
        get() = _binding ?: throw RuntimeException("Fragment is null")

    private val viewModel: TimerScreenViewModel by viewModel()
    private var serviceBound = false
    private var timerService: TimerService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as TimerService.LocalBinder
            timerService = localBinder.getService()
            viewModel.bindService(timerService!!)
            serviceBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerScreenBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        Intent(requireContext(), TimerService::class.java).also { intent ->
            requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            requireContext().unbindService(serviceConnection)
            serviceBound = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.state.collect { state ->
                when (state) {
                    is TimerState.Idle -> {
                        binding.textViewCountdown.text = "00:00:00"
                        binding.startPauseBtn.text = "Старт"
                        binding.resetBtn.visibility = View.INVISIBLE
                    }

                    is TimerState.Running -> {
                        binding.textViewCountdown.text = formatTime(state.timeLeft)
                        binding.startPauseBtn.text = "Пауза"
                        binding.resetBtn.visibility = View.VISIBLE
                    }

                    is TimerState.Paused -> {
                        binding.textViewCountdown.text = formatTime(state.timeLeft)
                        binding.startPauseBtn.text = "Продолжить"
                        binding.resetBtn.visibility = View.VISIBLE
                    }

                    is TimerState.Finished -> {
                        binding.textViewCountdown.text = "00:00:00"
                        binding.startPauseBtn.text = "Старт"
                        binding.resetBtn.visibility = View.INVISIBLE
                    }
                }
            }
        }

    }

    private fun formatTime(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun setupListeners() {
        binding.setTimeBtn.setOnClickListener {
            val seconds = binding.editTextInput.text.toString().toLongOrNull() ?: 0L
            viewModel.startTimer(seconds)
        }

        binding.startPauseBtn.setOnClickListener {
            val seconds = binding.textViewCountdown.text.split(":").map { it.toInt() }
                .let { it[0] * 3600 + it[1] * 60 + it[2] }

            when (val state = viewModel.state.value) {
                is TimerState.Running -> viewModel.pauseTimer()
                is TimerState.Paused -> viewModel.startTimer(state.timeLeft)
                is TimerState.Idle -> viewModel.startTimer(seconds.toLong())
                else -> Unit
            }
        }

        binding.resetBtn.setOnClickListener {
            viewModel.stopTimer()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TimerScreenFragment()
    }
}
