package top.neila.slimevanilla.listeners.craft.playerdata.timetocraft

import org.bukkit.entity.HumanEntity
import org.bukkit.scheduler.BukkitTask

infix fun HumanEntity.pendTimeToCraftTask(task: BukkitTask) = pendingTimeToCraftTasks.set(uniqueId, task)
