package top.neila.slimevanilla.listeners

import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine
import org.bukkit.NamespacedKey
import top.neila.slimevanilla.Slimevanilla
import top.neila.slimevanilla.defines.multiBlockTitleKeyMap

/**
 * 生成某机器某条配方对应的原版配方书 key。
 * 注册（addSlimefunRecipes）与解锁（onMultiBlockInteract）必须使用同一规则，
 * 否则配方书无法正确显示已解锁的配方。
 *
 * key 使用「机器标识 + 配方在 getRecipes() 扁平列表里的输入索引」。
 * 之所以不用产物 SF id，是因为：
 *  1) 同一产物可由多台机器产出（碳：压缩机 / 磨石），用产物 id 会让两台机器争抢同 key；
 *  2) 同一机器同一产物也可有多个不同输入的配方（压缩机：煤矿块×8→碳×9、
 *     煤炭×8→碳×1；磨石：钻石→碳），用产物 id 只会注册其中一条，其余被跳过，
 *     导致点开看到的是别的配方、且扣减数量取自错误配方（只扣 1 个）。
 * 用原始输入索引可保证每条配方都有唯一、稳定、与注册/解锁两边完全一致的 key。
 */
fun MultiBlockMachine.getRecipeKey(inputIndex: Int): NamespacedKey {
    val machineKey = multiBlockTitleKeyMap[this::class] ?: javaClass.simpleName.lowercase()
    return NamespacedKey(Slimevanilla.instance!!, "${machineKey}_$inputIndex")
}
