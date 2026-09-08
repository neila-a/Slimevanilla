package top.neila.slimevanilla.defines.ores

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems
import org.bukkit.Material

/**
 * 粘液里「有金属锭与金属粉、却没有矿物」的六种金属：锡、银、铅、铝、锌、镁。
 *
 * 原版只有铁/金/铜三套「矿石 + 粗金属 + 粗金属块」，因此这里的外观（Material）全部
 * 复用原版材质占位，同材质的多个物品靠显示名区分 —— 这与粘液自身的做法一致
 * （TIN_DUST / SILVER_DUST / ZINC_DUST 同为 SUGAR，全靠名字区分）。
 * 日后若要换成资源包或 CustomModelData，只需改这一个枚举。
 *
 * @property idPrefix 物品 id 前缀，派生出 SV_<ID>_ORE / SV_RAW_<ID> / SV_RAW_<ID>_BLOCK
 * @property displayName 中文名，用于派生「锡矿石」「粗锡」「粗锡块」
 * @property ore 矿石物品的外观材质占位
 * @property raw 粗金属物品的外观材质占位
 * @property rawBlock 粗金属块物品的外观材质占位
 * @property dust 该金属对应的粘液金属粉，用于碎矿机配方产出
 */
enum class Metal(
    val idPrefix: String,
    val displayName: String,
    val ore: Material,
    val raw: Material,
    val rawBlock: Material,
    val dust: SlimefunItemStack,
) {
    TIN("TIN", "锡", Material.IRON_ORE, Material.RAW_IRON, Material.RAW_IRON_BLOCK, SlimefunItems.TIN_DUST),
    SILVER("SILVER", "银", Material.DIAMOND_ORE, Material.RAW_IRON, Material.RAW_IRON_BLOCK, SlimefunItems.SILVER_DUST),
    LEAD("LEAD", "铅", Material.COAL_ORE, Material.RAW_COPPER, Material.RAW_COPPER_BLOCK, SlimefunItems.LEAD_DUST),
    ALUMINUM("ALUMINUM", "铝", Material.LAPIS_ORE, Material.RAW_IRON, Material.RAW_IRON_BLOCK, SlimefunItems.ALUMINUM_DUST),
    ZINC("ZINC", "锌", Material.REDSTONE_ORE, Material.RAW_COPPER, Material.RAW_COPPER_BLOCK, SlimefunItems.ZINC_DUST),
    MAGNESIUM("MAGNESIUM", "镁", Material.GOLD_ORE, Material.RAW_GOLD, Material.RAW_GOLD_BLOCK, SlimefunItems.MAGNESIUM_DUST),
}
