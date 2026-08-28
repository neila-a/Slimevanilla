package top.neila.slimevanilla.listeners.craft

import java.util.concurrent.ThreadLocalRandom
import org.bukkit.entity.Player

/* 冶炼炉（Smeltery / MakeshiftSmeltery）：还原原版「火随机被扑灭，自动打火机消耗打火石耐久重新点火」。
 * 原版 Slimefun 的 Smeltery.craft 有 fireBreakingChance（默认 34%）概率扑火，
 * 扑火时去点火室找 FLINT_AND_STEEL 耐久-1 重新点火；我们的合成台架构没有点火室方块，
 * 故改为：合成时按 34% 概率尝试消耗玩家背包里的打火石耐久；背包无打火石则该次合成取消
 * （等价于「火没点着」，材料不被消耗，提示玩家需要打火石）。 */
fun Player.smelteryIgniteOnce(): Boolean {
    return ThreadLocalRandom.current().nextInt(100) >= 34 || consumeFlintAndSteel()
}
