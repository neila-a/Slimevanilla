package top.neila.slimevanilla.listeners.craft.playerdata.opening

import org.bukkit.entity.HumanEntity

fun HumanEntity.close() = playerOpening.remove(uniqueId)
