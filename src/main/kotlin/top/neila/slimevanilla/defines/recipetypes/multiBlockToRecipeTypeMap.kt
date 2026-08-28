package top.neila.slimevanilla.defines.recipetypes

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientAltar
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.*

val multiBlockToRecipeTypeMap = mapOf(
    EnhancedCraftingTable::class to RecipeType.ENHANCED_CRAFTING_TABLE,
    AncientAltar::class to RecipeType.ANCIENT_ALTAR,
    ArmorForge::class to RecipeType.ARMOR_FORGE,
    Compressor::class to RecipeType.COMPRESSOR,
    GrindStone::class to RecipeType.GRIND_STONE,
    Juicer::class to RecipeType.JUICER,
    MagicWorkbench::class to RecipeType.MAGIC_WORKBENCH,
    OreCrusher::class to RecipeType.ORE_CRUSHER,
    OreWasher::class to RecipeType.ORE_WASHER,
    Smeltery::class to RecipeType.SMELTERY,
    MakeshiftSmeltery::class to RecipeType.SMELTERY,
    PressureChamber::class to RecipeType.PRESSURE_CHAMBER
)
