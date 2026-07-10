package vintagebadger.trainingplanner.data

import net.runelite.api.Client
import net.runelite.api.ItemContainer
import net.runelite.api.events.ItemContainerChanged
import net.runelite.api.gameval.InventoryID
import net.runelite.client.callback.ClientThread
import net.runelite.client.game.ItemManager
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import javax.swing.SwingUtilities

sealed class OwnedQuantitySnapshotResult {
    data class Captured(val quantities: Map<Int, Long>) : OwnedQuantitySnapshotResult()
    object BankUnavailable : OwnedQuantitySnapshotResult()
}

@Singleton
class OwnedQuantitySnapshotService @Inject constructor(
    private val client: Client,
    private val clientThread: ClientThread,
    private val itemManager: ItemManager,
) {
    private var lastBankQuantities: Map<Int, Long>? = null

    fun onItemContainerChanged(event: ItemContainerChanged) {
        if (event.containerId == InventoryID.BANK) {
            lastBankQuantities = quantitiesIn(event.itemContainer)
        }
    }

    fun clearBankSnapshot() {
        lastBankQuantities = null
    }

    fun capture(callback: (OwnedQuantitySnapshotResult) -> Unit) {
        clientThread.invoke(Runnable {
            val bank = lastBankQuantities
                ?: client.getItemContainer(InventoryID.BANK)?.let(::quantitiesIn)
            val result = if (bank == null) {
                OwnedQuantitySnapshotResult.BankUnavailable
            } else {
                val inventory = client.getItemContainer(InventoryID.INV)
                    ?.let(::quantitiesIn)
                    .orEmpty()
                OwnedQuantitySnapshotResult.Captured(combineQuantityMaps(bank, inventory))
            }
            SwingUtilities.invokeLater { callback(result) }
        })
    }

    private fun quantitiesIn(container: ItemContainer): Map<Int, Long> {
        val quantities = mutableMapOf<Int, Long>()
        container.items.forEach { item ->
            if (item.id < 0 || item.quantity <= 0) return@forEach
            val itemId = itemManager.canonicalize(item.id)
            quantities[itemId] = Math.addExact(
                quantities[itemId] ?: 0L,
                item.quantity.toLong(),
            )
        }
        return Collections.unmodifiableMap(quantities)
    }
}

internal fun combineQuantityMaps(
    bank: Map<Int, Long>,
    inventory: Map<Int, Long>,
): Map<Int, Long> {
    val combined = bank.toMutableMap()
    inventory.forEach { (itemId, quantity) ->
        combined[itemId] = Math.addExact(combined[itemId] ?: 0L, quantity)
    }
    return combined
}
