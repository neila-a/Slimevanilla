package top.neila.slimevanilla.defines.recipetypes

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType.*
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientAltar
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.*

val multiBlockToRecipeTypeMap = mapOf(
    EnhancedCraftingTable::class to ENHANCED_CRAFTING_TABLE,
    AncientAltar::class to ANCIENT_ALTAR,
    ArmorForge::class to ARMOR_FORGE,
    Compressor::class to COMPRESSOR,
    GrindStone::class to GRIND_STONE,
    Juicer::class to JUICER,
    MagicWorkbench::class to MAGIC_WORKBENCH,
    OreCrusher::class to ORE_CRUSHER,
    OreWasher::class to ORE_WASHER,
    Smeltery::class to SMELTERY,
    MakeshiftSmeltery::class to SMELTERY,
    PressureChamber::class to PRESSURE_CHAMBER
)
