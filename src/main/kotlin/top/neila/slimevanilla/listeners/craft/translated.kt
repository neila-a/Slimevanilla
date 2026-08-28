package top.neila.slimevanilla.listeners.craft

import net.kyori.adventure.translation.GlobalTranslator
import java.util.Locale

/**
 * 从已注册的国际化翻译文件（Bundle_zh_CN.properties）读取指定 key 的文本。  
 * 若翻译缺失则返回 key 本身，便于在没有翻译时仍能看到原始标识。
 */
val String.translated
    get(): String {
        val format = GlobalTranslator.translator().translate(this, Locale.SIMPLIFIED_CHINESE)
        return format?.format(emptyArray<Any>()) ?: this
    }
