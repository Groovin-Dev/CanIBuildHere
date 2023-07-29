package dev.groovin.canibuildhere

import com.griefdefender.api.GriefDefender
import com.griefdefender.api.claim.Claim
import com.griefdefender.lib.flowpowered.math.vector.Vector3i
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Location
import org.bukkit.entity.Player

class ClaimDetector(private val radius: Int) {
    fun getNearbyClaims(player: Player): List<Claim> {
        // First, get the core and claimManager for the players world
        val core = GriefDefender.getCore()
        val claimManager = core.getClaimManager(player.world.uid)
        if (claimManager == null) {
            println("Claim manager is null (idk how)")
            return emptyList()
        }

        // Then, get all the claims in the players world and remove the ones created by the player
        val allWorldClaims = claimManager.worldClaims
            .filter { it.ownerUniqueId != player.uniqueId }

        // Then, get the players location
        val playerLocation = player.location

        // Then, get the claims within the radius of the player
        return allWorldClaims.filter { isClaimNearPlayer(it, playerLocation) }
    }

    private fun isClaimNearPlayer(claim: Claim, playerLocation: Location): Boolean {
        val claimCenter = claim.lesserBoundaryCorner.add(claim.greaterBoundaryCorner).div(2)
        val distanceBetweenClaimAndPlayer = claimCenter.distance(locationToVector3(playerLocation))
        return distanceBetweenClaimAndPlayer <= radius
    }

    private fun locationToVector3(location: Location): Vector3i {
        return Vector3i(location.blockX, location.blockY, location.blockZ)
    }

    fun notifyUser(player: Player, claims: List<Claim>) {
        val claimCount = claims.size
        val claimCountComponent = Component.text(claimCount, NamedTextColor.YELLOW, TextDecoration.BOLD)
        val claimOwners = claims.map { it.ownerName }.distinct().joinToString(", ")
        val claimOwnersComponent = Component.text(claimOwners, NamedTextColor.BLUE, TextDecoration.BOLD)

        val message = Component.text("Sorry, you can't build here! There are currently ")
            .append(claimCountComponent)
            .append(Component.text(" claims near you by "))
            .append(claimOwnersComponent)
            .append(Component.text("."))

        player.sendMessage(message)
    }
}