package me.legosteenjaap.reworkedrivers;

import me.legosteenjaap.reworkedrivers.mixin.ChunkStatusesMixin;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.features.CaveFeatures;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.worldgen.biome.api.BiomeModifications;
import org.quiltmc.qsl.worldgen.biome.api.BiomeSelectors;
import org.quiltmc.qsl.worldgen.biome.api.ModificationPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReworkedRivers implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.

	public static final String MOD_ID = "reworked_rivers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize(ModContainer mod) {
		BiomeModifications.create(new ResourceLocation(MOD_ID, "remove_springs")).add(ModificationPhase.REMOVALS, BiomeSelectors.all(), (s) -> {
			s.getGenerationSettings().removeBuiltInFeature(MiscOverworldPlacements.SPRING_LAVA_FROZEN.value());
			s.getGenerationSettings().removeBuiltInFeature(MiscOverworldPlacements.SPRING_WATER.value());
			s.getGenerationSettings().removeBuiltInFeature(MiscOverworldPlacements.SPRING_LAVA.value());
		});
	}
}
