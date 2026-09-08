package top.neila.slimevanilla.defines.ores

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import top.neila.slimevanilla.core.Slimevanilla

/** 本插件物品 id 的统一前缀，避免与粘液或其它附属的 id 冲突 */
private const val SV = "SV_"

val Metal.oreId: String
    get() = "$SV${idPrefix}_ORE"

val Metal.rawId: String
    get() = "${SV}RAW_$idPrefix"

val Metal.rawBlockId: String
    get() = "${SV}RAW_${idPrefix}_BLOCK"

/*
 * 三件套物品。用 Map 缓存而非每次新建：SlimefunItemStack 注册后会被 lock，
 * 且配方里多处引用同一个实例，必须保证「同一个物品只有一个 SlimefunItemStack」。
 */
private val oreItems = Metal.entries.associateWith {
    SlimefunItemStack(it.oreId, it.ore, "&f${it.displayName}矿石")
}

private val rawItems = Metal.entries.associateWith {
    SlimefunItemStack(it.rawId, it.raw, "&f粗${it.displayName}")
}

private val rawBlockItems = Metal.entries.associateWith {
    SlimefunItemStack(it.rawBlockId, it.rawBlock, "&f粗${it.displayName}块")
}

val Metal.oreItem: SlimefunItemStack
    get() = oreItems.getValue(this)

val Metal.rawItem: SlimefunItemStack
    get() = rawItems.getValue(this)

val Metal.rawBlockItem: SlimefunItemStack
    get() = rawBlockItems.getValue(this)

/** 本插件全部矿物物品的 id 集合，用于判定某个物品是否属于本插件（如熔炉拦截） */
val oreItemIds: Set<String> = Metal.entries.flatMapTo(mutableSetOf()) {
    listOf(it.oreId, it.rawId, it.rawBlockId)
}

/**
 * 名为「矿石」的物品组。
 *
 * 直接 new 一个本插件专属的 ItemGroup，不复用粘液的「资源」组：
 * 矿物自成一类，指南里更好找。首个物品注册时（SlimefunItem#onEnable）粘液会自动
 * 调用 itemGroup.register(addon)，因此这里无需手动注册。
 */
val oreItemGroup = ItemGroup(
    NamespacedKey(Slimevanilla, "ores"),
    ItemStack(Material.IRON_ORE).apply { editMeta { it.setDisplayName("§f矿石") } }
)
