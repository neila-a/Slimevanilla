package top.neila.slimevanilla.listeners.craft.playerdata.timetocraft

import org.bukkit.entity.HumanEntity

/**
 * 关闭合成界面时取消挂起的 time to craft 延迟任务，避免对失效库存设 result
 */
fun HumanEntity.cancelTimeToCraftTask() = pendingTimeToCraftTasks.remove(uniqueId)?.cancel()
