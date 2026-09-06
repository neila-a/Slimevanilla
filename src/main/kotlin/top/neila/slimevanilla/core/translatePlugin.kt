package top.neila.slimevanilla.core

import net.kyori.adventure.translation.GlobalTranslator.translator
import net.kyori.adventure.translation.TranslationStore.messageFormat
import org.bukkit.NamespacedKey
import java.util.Locale.SIMPLIFIED_CHINESE
import java.util.ResourceBundle.getBundle
import top.neila.slimevanilla.core.Slimevanilla

fun translatePlugin() {
    val store = messageFormat(NamespacedKey(Slimevanilla, "translation_store"))

    val bundle = getBundle("top.neila.slimevanilla.Bundle", SIMPLIFIED_CHINESE)
    store.registerAll(SIMPLIFIED_CHINESE, bundle, true)
    translator().addSource(store)
}
