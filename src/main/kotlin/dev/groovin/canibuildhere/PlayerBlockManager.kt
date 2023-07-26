import dev.groovin.canibuildhere.BuildDetectorEngine
import net.coreprotect.CoreProtect
import net.coreprotect.CoreProtectAPI
import net.coreprotect.CoreProtectAPI.ParseResult
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit.getServer
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

data class Build(val player: Player, var size: Int, val location: Location)

class PlayerBlockManager {
    // Code to get CoreProtect
    private fun getCoreProtect(): CoreProtectAPI? {
        val plugin: Plugin? = getServer().pluginManager.getPlugin("CoreProtect")

        // Check that CoreProtect is loaded
        if (plugin == null || plugin !is CoreProtect) {
            println("CoreProtect is not loaded")
            return null
        }

        // Check that the API is enabled
        val coreProtectAPI = plugin.api
        if (!coreProtectAPI.isEnabled) {
            println("CoreProtect API is not enabled")
            return null
        }

        // Check that a compatible version of the API is loaded
        return if (coreProtectAPI.APIVersion() < 9) {
            println("Incompatible API version")
            null
        } else {
            println("CoreProtect API is loaded")
            coreProtectAPI
        }
    }

    // Method that determines if player is near player blocks
    fun isPlayerNearPlayerBlocks(player: Player): Boolean {
        val api = getCoreProtect() ?: throw Exception("CoreProtect is not loaded")
        val parsedLookup = lookupBlockChangesNearPlayer(api, player)
        val buildDetectorEngine = BuildDetectorEngine(10, 1.0)
        val builds = buildDetectorEngine.detectBuilds(parsedLookup, player)

        if (builds.isNotEmpty()) {
            notifyUser(player, builds)
        }

        return builds.isNotEmpty()
    }

    // Function to look up block changes near the player
    private fun lookupBlockChangesNearPlayer(api: CoreProtectAPI, player: Player): List<ParseResult> {
        val year = (60 * 60 * 24) * 365
        val restrictedBlocks = listOf(Material.DIRT, Material.GRASS_BLOCK, Material.GRASS, Material.STONE, Material.COBBLESTONE)
        val actions = mutableListOf(0, 1) // Block placement and block break

        // Search for all block changes near the player
        val lookup: List<Array<String?>> =
            api.performLookup(year, null, null, null, restrictedBlocks, actions, 100, player.location)

        val filteredLookup = lookup.filter { it[1]?.startsWith("#") == false }
        val parsedLookup: List<ParseResult> = filteredLookup.map { api.parseResult(it) }

        println("Found ${parsedLookup.size} block changes")

        val filteredBlocks = filterPlacedBlocks(parsedLookup, player.world.name)
        println("Filtered to ${filteredBlocks.size} placed blocks")

        return filteredBlocks
    }

    // Function to filter placed blocks
    private fun filterPlacedBlocks(parsedLookup: List<ParseResult>, worldName: String): List<ParseResult> {
        return parsedLookup.filter { result ->
            val location =
                Location(getServer().getWorld(worldName), result.x.toDouble(), result.y.toDouble(), result.z.toDouble())
            val block = location.block
            block.type == result.type
        }
    }

    // Function to notify the user
    private fun notifyUser(player: Player, builds: List<dev.groovin.canibuildhere.Build>) {
        val buildCount = Component.text(builds.size, NamedTextColor.YELLOW, TextDecoration.BOLD)
        val builders = builds.map { it.player }.distinct().joinToString(", ")
        val builderComponent = Component.text(builders, NamedTextColor.BLUE, TextDecoration.BOLD)

        val message = Component.text("Sorry, you can't build here! There are currently ")
            .append(buildCount)
            .append(Component.text(" builds near you by "))
            .append(builderComponent)
            .append(Component.text("."))

        player.sendMessage(message)
    }
}
