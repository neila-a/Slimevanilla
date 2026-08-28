package top.neila.slimevanilla.listeners.craft

import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile
import org.bukkit.entity.HumanEntity
import java.util.function.Consumer

fun HumanEntity.toSlimefun(callback: Consumer<PlayerProfile>) = PlayerProfile.fromUUID(uniqueId, callback)
