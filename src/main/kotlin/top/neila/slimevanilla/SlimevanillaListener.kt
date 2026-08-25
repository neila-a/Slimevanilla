package top.neila.slimevanilla

import io.github.thebusybiscuit.slimefun4.api.events.MultiBlockInteractEvent
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.Event.Result
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.EventPriority
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MenuType.CRAFTING
import org.bukkit.event.inventory.CraftItemEvent
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientAltar
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.ArmorForge
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.Compressor
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.EnhancedCraftingTable
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.GrindStone
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.Juicer
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.MagicWorkbench
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.MakeshiftSmeltery
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.OreCrusher
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.OreWasher
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.PressureChamber
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.Smeltery
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.NamespacedKey
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import java.util.Locale
import java.util.UUID
import java.util.function.Consumer

val emptyMatrix = arrayOfNulls<ItemStack>(9)
val Array<ItemStack?>.flat
    get() = map { stack ->
        if (stack == null) return@map null
        val flatStack = stack.clone()
        flatStack.amount = 1
        return@map flatStack
    }

fun HumanEntity.toSlimefun(callback: Consumer<PlayerProfile>) = PlayerProfile.fromUUID(uniqueId, callback)

class SlimevanillaListener(instance: Slimevanilla) : Listener {
    private var slimevanillaInstance: Slimevanilla? = null

    init {
        slimevanillaInstance = instance
        instance.server.pluginManager.registerEvents(this, instance)
    }

    private val playerVanillaDiscoverdRecipes = mutableMapOf<UUID, Collection<NamespacedKey>>()
    private val playerOpening = mutableMapOf<UUID, RecipeType?>()

    @EventHandler(priority = EventPriority.LOW)
    fun onMultiBlockInteract(event: MultiBlockInteractEvent) {
        val multiBlock = event.multiBlock
        val player = event.player
        val item = multiBlock.slimefunItem

        var type: RecipeType? = null
        if (item is EnhancedCraftingTable) {
            type = RecipeType.ENHANCED_CRAFTING_TABLE
        } else if (item is AncientAltar) {
            type = RecipeType.ANCIENT_ALTAR
        } else if (item is ArmorForge) {
            type = RecipeType.ARMOR_FORGE
        } else if (item is Compressor) {
            type = RecipeType.COMPRESSOR
        } else if (item is GrindStone) {
            type = RecipeType.GRIND_STONE
        } else if (item is Juicer) {
            type = RecipeType.JUICER
        } else if (item is MagicWorkbench) {
            type = RecipeType.MAGIC_WORKBENCH
        } else if (item is OreCrusher) {
            type = RecipeType.ORE_CRUSHER
        } else if (item is OreWasher) {
            type = RecipeType.ORE_WASHER
        } else if (item is Smeltery || item is MakeshiftSmeltery) {
            type = RecipeType.SMELTERY
        } else if (item is PressureChamber) {
            type = RecipeType.PRESSURE_CHAMBER
        }

        player.toSlimefun { profile ->
            if (!profile.hasUnlocked(item.research)) return@toSlimefun

            if (type != null) {
                event.isCancelled = true

                playerVanillaDiscoverdRecipes[player.uniqueId] = player.discoveredRecipes
                player.undiscoverRecipes(player.discoveredRecipes)
                player.discoverRecipes(profile.researches.map { research ->
                    research.affectedItems.map { item ->
                        if (item.recipeType == type) {
                            val key = NamespacedKey(slimevanillaInstance!!, item.id)
                            return@map key
                        }
                        return@map null
                    }.filterNotNull()
                }.flatten())

                playerOpening[player.uniqueId] = type
                val view = player.openWorkbench(event.clickedBlock.location, true)

                val format =
                    GlobalTranslator.translator().translate("container.enhanced_crafting", Locale.SIMPLIFIED_CHINESE)
                val title = format?.format(emptyArray<Any>())
                if (title != null)
                    view?.title = title
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player
        if (playerOpening[player.uniqueId] == null) return
        val playerVanillaDiscoverdRecipe = playerVanillaDiscoverdRecipes[player.uniqueId] ?: return
        player.undiscoverRecipes(player.discoveredRecipes)
        player.discoverRecipes(playerVanillaDiscoverdRecipe)
        playerOpening[player.uniqueId] = null
    }

    // io.github.thebusybiscuit.slimefun4.implementation.listeners.crafting.CraftingTableListener#onPrepareCraft priority = NORMAL
    @EventHandler(priority = EventPriority.HIGH)
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        val inventory = event.inventory

        val matrix = inventory.matrix
        if (matrix.contentEquals(emptyMatrix)) return

        val players = inventory.viewers.filterNotNull()
        if (players.isEmpty()) return
        val player = players.component1()


        if (playerOpening[player.uniqueId] != null) {
            inventory.result = null

            player.toSlimefun { profile ->
                val flatMatrix = matrix.flat
                Slimefun.getRegistry().enabledSlimefunItems.filterNotNull().forEach { item ->
                    if (item.recipeType != playerOpening[player.uniqueId]) return@forEach

                    val research = item.research
                    if (!profile.hasUnlocked(research)) return@forEach

                    if (item.recipe.flat == flatMatrix) {
                        inventory.result = item.recipeOutput
                    }
                }
            }
        }
    }

    // io.github.thebusybiscuit.slimefun4.implementation.listeners.crafting.CraftingTableListener#onCraft priority = NORMAL
    @EventHandler(priority = EventPriority.HIGH)
    fun onCraft(event: CraftItemEvent) {
        if (event.inventory.result != null) {
            // If it's really using slimefun items to craft vanilla items
            // result will be null.
            // And if result isn't null
            // It's crafting with/to slimefun items.
            event.result = Result.ALLOW
            event.isCancelled = false
        }
    }
}
