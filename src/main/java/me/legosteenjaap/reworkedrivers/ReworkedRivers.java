package me.legosteenjaap.reworkedrivers;

import me.legosteenjaap.reworkedrivers.river.RiverBendType;
import me.legosteenjaap.reworkedrivers.river.RiverDirection;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.resources.ResourceLocation;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.worldgen.biome.api.BiomeModifications;
import org.quiltmc.qsl.worldgen.biome.api.BiomeSelectors;
import org.quiltmc.qsl.worldgen.biome.api.ModificationPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReworkedRivers implements ModInitializer {

	public static final boolean DEBUG_PIECES = false;
	public static final boolean DEBUG_HEIGHT = false;
	public static final boolean DEBUG_CONNECTION = false;
	public static final RiverDirection DEBUG_RIVER_DIRECTION = RiverDirection.NORTH;
	public static final RiverBendType DEBUG_BEND_TYPE = RiverBendType.RIGHT;

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
