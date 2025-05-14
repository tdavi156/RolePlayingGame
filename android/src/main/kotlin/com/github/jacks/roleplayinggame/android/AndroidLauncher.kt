package com.github.jacks.roleplayinggame.android

import android.os.Bundle

import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.github.jacks.roleplayinggame.RolePlayingGame

/** Launches the Android application. */
class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize(RolePlayingGame(), AndroidApplicationConfiguration().apply {
            // Configure your application here.
            useImmersiveMode = false // Recommended, but not required.
        })
    }
}
