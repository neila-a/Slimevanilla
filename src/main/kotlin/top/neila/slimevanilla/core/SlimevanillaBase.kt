package top.neila.slimevanilla.core

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon
import java.util.logging.Logger

/**
 * 重新声明 {@link SlimefunAddon} 中与 {@link org.bukkit.plugin.PluginBase} 冲突的成员。
 *
 * {@code PluginBase.getName()} 在 Java 中是 {@code final}，Kotlin 既不能重写它、
 * 又会在「final 类成员 + 接口默认方法」同时出现时强制要求显式重写，因此无法直接
 * 写出 {@code class X : JavaPlugin(), SlimefunAddon}。
 * 这里把接口的 {@code default} 方法重新声明为抽象方法后，实现便只来自
 * {@code JavaPlugin} 一侧，冲突消失；JVM 解析 {@code invokeinterface} 时同样会
 * 优先命中类继承链上的实现，行为与原先的 Java 中间类完全一致。
 */
interface SlimevanillaBase : SlimefunAddon {
    override fun getName(): String
    override fun getLogger(): Logger
}
