package dev.groovin.canibuildhere

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

                val claimDetector = ClaimDetector(200)
                val nearbyClaims = claimDetector.getNearbyClaims(sender as Player)
                if (nearbyClaims.isNotEmpty()) {
                    claimDetector.notifyUser(sender as Player, nearbyClaims)
                    return@executes
                }

                val playerBlockManager = PlayerBlockManager()
                val nearbyBuilds = playerBlockManager.getNearbyBuilds(sender as Player)
                if (nearbyBuilds.isNotEmpty()) {
                    playerBlockManager.notifyUser(sender as Player, nearbyBuilds)
                    return@executes
                }

                sender.sendMessage(Component.text("You can build here!"))
            }
        }
    }
}