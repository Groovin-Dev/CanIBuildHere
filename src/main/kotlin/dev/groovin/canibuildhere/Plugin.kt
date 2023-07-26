package dev.groovin.canibuildhere

import io.github.monun.kommand.kommand
import org.bukkit.plugin.java.JavaPlugin

lateinit var instance: Plugin

class Plugin : JavaPlugin() {
    override fun onEnable() {
        instance = this
        kommand {
            CommandDispatcher.register(this)
        }
    }
}