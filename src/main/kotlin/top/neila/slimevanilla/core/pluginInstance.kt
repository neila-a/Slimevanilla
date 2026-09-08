package top.neila.slimevanilla.core

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

/**
 * 当前已启用的插件实例。
 *
 * 主类是 Kotlin 的 `object`，而 Paper 的 PluginBootstrap 会让它被两个类加载器各加载一份：
 * 由 `createPlugin()` 返回、真正被启用的那一份（A），以及插件自身的类加载器里另外一份（B）。
 * 各处直接引用 `Slimevanilla` 拿到的是 B，它并没有被启用 —— 用它注册监听器或调度任务会抛
 * `IllegalPluginAccessException`。因此凡是需要「插件实例」的场合（registerEvents、runTask、
 * runTaskLater、NamespacedKey 等）都应使用这里解析出来的已启用实例。
 */
val pluginInstance: JavaPlugin
    get() = Bukkit.getPluginManager().getPlugin("Slimevanilla") as? JavaPlugin ?: Slimevanilla
