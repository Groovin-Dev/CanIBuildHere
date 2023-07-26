import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import java.util.*

data class BlockWrapper(val block: Block) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BlockWrapper
        return block.location == other.block.location
    }

    override fun hashCode(): Int {
        return block.location.hashCode()
    }
}

object PlayerBlockTracker : Listener {

    private val placedBlocks: MutableList<BlockWrapper> = Collections.synchronizedList(mutableListOf())

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val block = event.block

        // Log the block placed
        println("Block placed: ${block.type}")

        synchronized(placedBlocks) {
            placedBlocks.add(BlockWrapper(block))
        }

        // Log the list of placed blocks
        println("[onBlockPlace] Placed blocks: $placedBlocks")
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block

        // Log the block broken
        println("Block broken: ${block.type}")

        synchronized(placedBlocks) {
            placedBlocks.remove(BlockWrapper(block))
        }

        // Log the list of placed blocks
        println("[onBlockBreak] Placed blocks: $placedBlocks")
    }

    fun isNearPlayerPlacedBlock(radius: Int, player: Player): Boolean {
        // Log the list of placed blocks
        println("[isNearPlayerPlacedBlock] Placed blocks: $placedBlocks")

        // Check if any of the placed blocks are within the radius of the player
        return synchronized(placedBlocks) {
            placedBlocks.any { player.location.distance(it.block.location) <= radius }
        }
    }
}
