package dev.groovin.canibuildhere

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

    fun getNearbyBuilds(player: Player): List<Build> {
        val api = getCoreProtect() ?: throw Exception("CoreProtect is not loaded")
        val parsedLookup = lookupBlockChangesNearPlayer(api, player)
        val buildDetectorEngine = BuildDetectorEngine(10, 1.0)

        return buildDetectorEngine.detectBuilds(parsedLookup, player)
    }

    private fun lookupBlockChangesNearPlayer(api: CoreProtectAPI, player: Player): List<ParseResult> {
        // It's stupid that I have to do this, but for some reason the API doesn't like a null time value
        // even though you don't have to provide one in the command. Maybe I'm just stupid tho lol
        val year = (60 * 60 * 24) * 365
        val decade = year * 10
        val restrictedBlocks = listOf(Material.DIRT, Material.GRASS_BLOCK, Material.GRASS, Material.STONE, Material.COBBLESTONE)
        val actions = mutableListOf(0, 1) // Block placement and block break

        // Search for all block changes near the player
        // For now I am just using a radius of 100 blocks. It's the max that CoreProtect allows but I could break it
        // into 4 quadrants and do 4 lookups to get a larger area.
        // TODO: Break into quadrants to get a larger area
        val lookup: List<Array<String?>> =
            api.performLookup(decade, null, null, null, restrictedBlocks, actions, 100, player.location)

        val filteredLookup = lookup.filter { it[1]?.startsWith("#") == false }
        val parsedLookup: List<ParseResult> = filteredLookup.map { api.parseResult(it) }

        println("Found ${parsedLookup.size} block changes")

        val filteredBlocks = filterPlacedBlocks(parsedLookup, player.world.name)
        println("Filtered to ${filteredBlocks.size} placed blocks")

        return filteredBlocks
    }

    private fun filterPlacedBlocks(parsedLookup: List<ParseResult>, worldName: String): List<ParseResult> {
        return parsedLookup.filter { result ->
            val location =
                Location(getServer().getWorld(worldName), result.x.toDouble(), result.y.toDouble(), result.z.toDouble())
            val block = location.block
            block.type == result.type
        }
    }

    fun notifyUser(player: Player, builds: List<Build>) {
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
