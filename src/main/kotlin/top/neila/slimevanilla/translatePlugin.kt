package top.neila.slimevanilla

import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationStore
import org.bukkit.NamespacedKey
import java.util.Locale
import java.util.ResourceBundle

fun translatePlugin() {
    val store = TranslationStore.messageFormat(NamespacedKey(Slimevanilla.instance!!, "translation_store"))

    val bundle = ResourceBundle.getBundle("top.neila.slimevanilla.Bundle", Locale.SIMPLIFIED_CHINESE)
    store.registerAll(Locale.SIMPLIFIED_CHINESE, bundle, true)
    GlobalTranslator.translator().addSource(store)
}
