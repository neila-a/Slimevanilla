package top.neila.slimevanilla;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Intermediate Java base class.
 *
 * Resolves the Kotlin/Java interop conflict between {@code PluginBase.getName()}
 * (which is {@code final}) and the {@code default} {@code getName()} provided by
 * the {@link SlimefunAddon} interface. In Java, the concrete (final) class method
 * takes precedence over the interface default, so no override is required here.
 * Kotlin subclasses then inherit a single, unambiguous implementation.
 */
public abstract class SlimevanillaBase extends JavaPlugin implements SlimefunAddon {
}
