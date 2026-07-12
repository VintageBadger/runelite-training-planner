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

data class OwnedQuantitySnapshot(
    val quantities: Map<Int, Long>,
    val includesBank: Boolean,
)

@Singleton
class OwnedQuantitySnapshotService @Inject constructor(
    private val client: Client,
    private val clientThread: ClientThread,
    private val itemManager: ItemManager,
) {
    private var lastBankQuantities: Map<Int, Long>? = null
    private var lastInventoryQuantities: Map<Int, Long>? = null
    @Volatile
    private var listener: ((OwnedQuantitySnapshot) -> Unit)? = null

    fun setListener(listener: (OwnedQuantitySnapshot) -> Unit) {
        this.listener = listener
        refresh()
    }

    fun clearListener() {
        listener = null
    }

    fun onItemContainerChanged(event: ItemContainerChanged) {
        val changed = when (event.containerId) {
            InventoryID.BANK -> {
                lastBankQuantities = quantitiesIn(event.itemContainer)
                true
            }
            InventoryID.INV -> {
                lastInventoryQuantities = quantitiesIn(event.itemContainer)
                true
            }
            else -> false
        }
        if (changed) publish()
    }

    fun clearSnapshots() {
        lastBankQuantities = null
        lastInventoryQuantities = null
        publish()
    }

    private fun refresh() {
        clientThread.invoke(Runnable {
            client.getItemContainer(InventoryID.INV)?.let {
                lastInventoryQuantities = quantitiesIn(it)
            }
            client.getItemContainer(InventoryID.BANK)?.let {
                lastBankQuantities = quantitiesIn(it)
            }
            publish()
        })
    }

    private fun publish() {
        if (listener == null) return
        val bank = lastBankQuantities
        val inventory = lastInventoryQuantities.orEmpty()
        val snapshot = OwnedQuantitySnapshot(
            quantities = if (bank == null) inventory else combineQuantityMaps(bank, inventory),
            includesBank = bank != null,
        )
        SwingUtilities.invokeLater { listener?.invoke(snapshot) }
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
