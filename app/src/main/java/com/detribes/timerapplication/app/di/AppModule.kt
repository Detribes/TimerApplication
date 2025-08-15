package com.detribes.timerapplication.app.di

import com.detribes.timerapplication.app.services.ServiceDelegate
import com.detribes.timerapplication.features.timerscreen.presentation.TimerScreenViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module


val appModule = module {
    single {
        ServiceDelegate()
    }

    viewModel {
        TimerScreenViewModel( get() )
    }
}