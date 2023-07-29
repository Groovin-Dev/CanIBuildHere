package dev.groovin.canibuildhere

import org.bukkit.block.Block
import net.coreprotect.CoreProtectAPI.ParseResult
import org.bukkit.World
import org.bukkit.entity.Player
import kotlin.math.abs

data class Build(val player: String, var size: Int, val blocks: MutableSet<Block>)

class BuildDetectorEngine(private val minBuildSize: Int, private val maxGap: Double) {

    fun detectBuilds(parseResults: List<ParseResult>, currentPlayer: Player): List<Build> {
        // Remove any blocks that were placed by the player we're checking for.
        val filteredParseResults = parseResults.filter { it.player != currentPlayer.name }

        println("Starting build detection for ${filteredParseResults.size} blocks")
        val builds = mutableListOf<Build>()
        val blocksGroupedByPlayers = filteredParseResults.groupBy { it.player }

        for ((playerName, playerBlocksParseResults) in blocksGroupedByPlayers) {
            println("Processing ${playerBlocksParseResults.size} blocks for player $playerName")
            val playerBlocks = playerBlocksParseResults.map { it.toBlock(currentPlayer.world) }
            processPlayerBlocks(builds, playerName, playerBlocks)
        }

        println("Finished build detection, found ${builds.size} builds")

        val buildsMeetingSizeThreshold = builds.filter { it.size >= minBuildSize }

        println("Number of builds meeting the size threshold: ${buildsMeetingSizeThreshold.size}")

        return buildsMeetingSizeThreshold
    }

    private fun ParseResult.toBlock(world: World): Block {
        return world.getBlockAt(this.x, this.y, this.z)
    }

    private fun addBuild(builds: MutableList<Build>, playerName: String, playerBuildBlocks: MutableSet<Block>) {
        val build = Build(playerName, playerBuildBlocks.size, playerBuildBlocks)
        builds.add(build)
    }

    private fun mergeBuilds(builds: MutableList<Build>, newBuild: Build): Build {
        val overlappingBuilds = builds.filter { it.player == newBuild.player && it.blocks.any { newBuildBlock -> newBuild.blocks.any { isBlockNearby(newBuildBlock, it) } } }
        builds.removeAll(overlappingBuilds)
        overlappingBuilds.forEach { newBuild.blocks.addAll(it.blocks) }
        newBuild.size = newBuild.blocks.size
        builds.add(newBuild) // Add the merged build back to the builds list.
        return newBuild
    }

    private fun processPlayerBlocks(builds: MutableList<Build>, playerName: String, playerBlocks: List<Block>) {
        val sortedPlayerBlocks = playerBlocks.sortedWith(compareBy({ it.x }, { it.y }, { it.z }))
        var currentBuildBlocks = mutableSetOf<Block>()
        var potentialBuildBlocks = mutableSetOf<Block>()

        for (block in sortedPlayerBlocks) {
            if (currentBuildBlocks.isEmpty() || currentBuildBlocks.any { isBlockNearby(it, block) }) {
                currentBuildBlocks.add(block)
                potentialBuildBlocks.add(block)
            } else if (potentialBuildBlocks.any { isBlockNearby(it, block) }) {
                potentialBuildBlocks.add(block)
            } else {
                addBuild(builds, playerName, currentBuildBlocks)
                currentBuildBlocks = potentialBuildBlocks
                potentialBuildBlocks = mutableSetOf(block)
            }
        }

        if (potentialBuildBlocks.isNotEmpty()) {
            currentBuildBlocks.addAll(potentialBuildBlocks)
        }

        if (currentBuildBlocks.isNotEmpty()) {
            addBuild(builds, playerName, currentBuildBlocks)
        }

        val overlappingBuilds = builds.filter { build -> builds.any { otherBuild -> otherBuild != build && otherBuild.player == build.player && build.blocks.any { block -> otherBuild.blocks.any { isBlockNearby(block, it) } } } }
        overlappingBuilds.forEach { mergeBuilds(builds, it) }
    }


    private fun isBlockNearby(block1: Block, block2: Block): Boolean {
        val distance = maxOf(abs(block1.x - block2.x), abs(block1.y - block2.y), abs(block1.z - block2.z))
        return distance <= maxGap
    }
}
