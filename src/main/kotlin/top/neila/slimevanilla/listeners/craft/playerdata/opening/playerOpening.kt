package top.neila.slimevanilla.listeners.craft.playerdata.opening

import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine
import java.util.UUID
import org.bukkit.inventory.InventoryView

val playerOpening = mutableMapOf<UUID, Pair<MultiBlockMachine, InventoryView>?>()
