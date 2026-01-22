@file:JvmName("Lwjgl3Launcher")

package com.github.jacks.roleplayinggame.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.github.jacks.roleplayinggame.RolePlayingGame

/** Launches the desktop (LWJGL3) application. */
fun main() {
    // This handles macOS support and helps on Windows.
    if (StartupHelper.startNewJvmIfRequired())
      return
    Lwjgl3Application(RolePlayingGame(), Lwjgl3ApplicationConfiguration().apply {
        setTitle("RolePlayingGame")
        setWindowedMode(1600, 900)
        setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))
    })
}

//Monitor primary = Lwjgl3ApplicationConfiguration.getPrimaryMonitor()
//DisplayMode desktopMode = Lwjgl3ApplicationConfiguration.getDisplayMode(monitor)
//DisplayMode primaryDesktopMode = Lwjgl3ApplicationConfiguration.getDisplayMode()
//Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration()
//config.setFullscreenMode(primaryMode)
//codeconfig.setResizable(false)
//config.setDecorated(false)
