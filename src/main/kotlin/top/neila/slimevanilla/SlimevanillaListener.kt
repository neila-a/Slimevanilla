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
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.EnhancedCraftingTable
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.NamespacedKey
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
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
    private val playerOpeningEC = mutableMapOf<UUID, Boolean>()

    @EventHandler(priority = EventPriority.LOW)
    fun onMultiBlockInteract(event: MultiBlockInteractEvent) {
        val multiBlock = event.multiBlock
        val player = event.player
        player.toSlimefun { profile ->
            val item = multiBlock.slimefunItem
            if (!profile.hasUnlocked(item.research)) return@toSlimefun

            if (item is EnhancedCraftingTable) {
                event.isCancelled = true

                playerVanillaDiscoverdRecipes[player.uniqueId] = player.discoveredRecipes
                player.undiscoverRecipes(player.discoveredRecipes)
                player.discoverRecipes(profile.researches.map { research ->
                    research.affectedItems.map { item ->
                        if (item.recipeType == RecipeType.ENHANCED_CRAFTING_TABLE) {
                            val key = NamespacedKey(slimevanillaInstance!!, item.id)
                            return@map key
                        }
                        return@map null
                    }.filterNotNull()
                }.flatten())

                playerOpeningEC[player.uniqueId] = true
                val view = player.openWorkbench(event.clickedBlock.location, true)
                
                val format = GlobalTranslator.translator().translate("container.enhanced_crafting", Locale.SIMPLIFIED_CHINESE)
                val title = format?.format(emptyArray<Any>())
                if (title != null)
                    view?.title = title
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player
        if (!(playerOpeningEC[player.uniqueId] ?: false)) return
        val playerVanillaDiscoverdRecipe = playerVanillaDiscoverdRecipes[player.uniqueId] ?: return
        player.undiscoverRecipes(player.discoveredRecipes)
        player.discoverRecipes(playerVanillaDiscoverdRecipe)
        playerOpeningEC[player.uniqueId] = false
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


        if (playerOpeningEC[player.uniqueId] ?: false) {
            // Opening EC

            // In EC can't craft vanilla items
            // NEED TEST
            inventory.result = null

            player.toSlimefun { profile ->
                val flatMatrix = matrix.flat
                Slimefun.getRegistry().enabledSlimefunItems.filterNotNull().forEach { item ->
                    if (item.recipeType != RecipeType.ENHANCED_CRAFTING_TABLE) return@forEach

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
