package com.rekosuo.gymtracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class / entrypoint.
 * HiltAndroidApp triggers code generation.
 */
@HiltAndroidApp
class GymTrackerApplication : Application()
