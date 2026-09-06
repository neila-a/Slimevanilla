package top.neila.slimevanilla.listeners.craft.playerdata.timetocraft

import org.bukkit.entity.HumanEntity

fun HumanEntity.isTimeToCrafting() = pendingTimeToCraftTasks.containsKey(uniqueId)
