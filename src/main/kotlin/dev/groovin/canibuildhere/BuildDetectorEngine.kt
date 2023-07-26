package dev.groovin.canibuildhere

import org.bukkit.Location
import org.bukkit.block.Block
import net.coreprotect.CoreProtectAPI.ParseResult
import org.bukkit.World
import org.bukkit.entity.Player
import kotlin.math.abs
import kotlin.math.sqrt

data class Build(val player: String, var size: Int, val blocks: MutableSet<Block>) {
    val center: Location
        get() {
            val x = blocks.map { it.location.x }.average()
            val y = blocks.map { it.location.y }.average()
            val z = blocks.map { it.location.z }.average()

            // Assuming all blocks are in the same world, so we just get the world from the first block.
            return Location(blocks.first().world, x, y, z)
        }
}

class BuildDetectorEngine(private val minBuildSize: Int, private val maxGap: Double) {

    fun detectBuilds(parseResults: List<ParseResult>, currentPlayer: Player): List<Build> {
        println("Starting build detection for ${parseResults.size} blocks")
        val builds = mutableListOf<Build>()
        val blocksGroupedByPlayers = parseResults.groupBy { it.player }

        for ((playerName, playerBlocksParseResults) in blocksGroupedByPlayers) {
            println("Processing ${playerBlocksParseResults.size} blocks for player $playerName")
            val playerBlocks = playerBlocksParseResults.map { it.toBlock(currentPlayer.world) }
            processPlayerBlocks(builds, playerName, playerBlocks)
        }

        println("Finished build detection, found ${builds.size} builds")

        val buildsMeetingSizeThreshold = builds.filter { it.size >= minBuildSize }

        println("Number of builds meeting the size threshold: ${buildsMeetingSizeThreshold.size}")

        for (build in buildsMeetingSizeThreshold) {
            println("Build of size ${build.size} for player ${build.player} at location ${build.center}")
            printBuild(build)
        }

        return buildsMeetingSizeThreshold
    }

    private fun ParseResult.toBlock(world: World): Block {
        // Convert ParseResult to Block. Replace this with your actual conversion logic.
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
        // println("Block1 xyz: ${block1.x}, ${block1.y}, ${block1.z}")
        // println("Block2 xyz: ${block2.x}, ${block2.y}, ${block2.z}")

        val distance = maxOf(abs(block1.x - block2.x), abs(block1.y - block2.y), abs(block1.z - block2.z))
        // println("Calculated distance between blocks as $distance")
        return distance <= maxGap
    }


    private fun distanceBetween(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble())
    }

    private fun printBuild(build: Build) {
        println("Printing build for player ${build.player}")

        val blocks = build.blocks
        val minX = blocks.minOf { it.x }
        val minY = blocks.minOf { it.y }
        val minZ = blocks.minOf { it.z }

        val maxX = blocks.maxOf { it.x }
        val maxY = blocks.maxOf { it.y }
        val maxZ = blocks.maxOf { it.z }

        val buildMatrix = Array(maxY - minY + 1) { Array(maxZ - minZ + 1) { Array(maxX - minX + 1) { "" } } }

        for (block in blocks) {
            val x = block.x - minX
            val y = block.y - minY
            val z = block.z - minZ

            buildMatrix[y][z][x] = block.type.name
        }

        for (y in 0 until maxY - minY + 1) {
            for (z in 0 until maxZ - minZ + 1) {
                for (x in 0 until maxX - minX + 1) {
                    print("[${buildMatrix[y][z][x].take(6).padEnd(12)}] ")
                }
                println()
            }
            println("-----")
        }
    }
}
