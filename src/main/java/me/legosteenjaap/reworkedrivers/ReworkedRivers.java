package me.legosteenjaap.reworkedrivers;

import me.legosteenjaap.reworkedrivers.mixin.ChunkStatusesMixin;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReworkedRivers implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.

	public static final String MOD_ID = "reworked_rivers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
	}
}
