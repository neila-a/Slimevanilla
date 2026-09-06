package top.neila.slimevanilla.listeners.craft.playerdata.opening

import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.InventoryView

fun HumanEntity.open(machine: MultiBlockMachine, view: InventoryView) {
    view.open()
    playerOpening[uniqueId] = machine to view
}
