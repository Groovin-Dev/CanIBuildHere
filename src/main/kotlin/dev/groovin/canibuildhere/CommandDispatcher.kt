package dev.groovin.canibuildhere

import PlayerBlockManager
import io.github.monun.kommand.PluginKommand
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

object CommandDispatcher {
    fun register(kommand: PluginKommand) {
        kommand.register("canibuildhere") {
            executes {
                if (sender !is Player) {
                    sender.sendMessage(Component.text("Only players can execute this command"))
                    return@executes
                }

                val playerBlockManager = PlayerBlockManager()
                val isPlayerNearPlayerBlocks = playerBlockManager.isPlayerNearPlayerBlocks(sender as Player)

                sender.sendMessage(Component.text("Is player near player blocks: $isPlayerNearPlayerBlocks"))
            }
        }
    }
}