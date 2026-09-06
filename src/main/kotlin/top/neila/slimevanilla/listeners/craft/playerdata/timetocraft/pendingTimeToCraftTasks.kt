package top.neila.slimevanilla.listeners.craft.playerdata.timetocraft

import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * time to craft：按玩家记录挂起的「延迟显示产物」任务，  
 * 避免每次 PrepareItemCraft 重复调度
 */
internal val pendingTimeToCraftTasks = mutableMapOf<UUID, BukkitTask>()
