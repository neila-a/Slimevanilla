package top.neila.slimevanilla.listeners.craft.playerdata.opening

import org.bukkit.entity.HumanEntity

val HumanEntity.opening
    get() = playerOpening[uniqueId]
