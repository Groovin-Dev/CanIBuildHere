package dev.groovin.canibuildhere

import PlayerBlockTracker
import dev.jorel.commandapi.*
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.NativeCommandExecutor
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class Plugin : JavaPlugin() {
    override fun onLoad() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(this).verboseOutput(true)) // Load with verbose output

        // Create our command
        CommandAPICommand("canibuildhere")
            .withAliases("cibh")
            .withPermission(CommandPermission.NONE)
            .executes(CommandExecutor { sender, args ->
                // Check if the sender is near a player-placed block
                val isNearPlayerPlacedBlock = PlayerBlockTracker.isNearPlayerPlacedBlock(200, sender as Player);

                // Send a message to the sender
                sender.sendMessage(Component.text("Is near player-placed block: $isNearPlayerPlacedBlock"))
            })
            .register()
    }

    override fun onEnable() {
        CommandAPI.onEnable()

        server.pluginManager.registerEvents(PlayerBlockTracker, this)
    }

    override fun onDisable() {
        CommandAPI.onDisable()
    }
}