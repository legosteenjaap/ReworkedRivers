package me.legosteenjaap.reworkedrivers.mixin;


import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.world.chunk.ChunkStatus.*;

@Mixin(ChunkStatus.class)
public abstract class DisableChunkStatusesMixin <T extends ChunkStatus> {

    @Shadow @Final public static ChunkStatus EMPTY;
    @Shadow @Final public static ChunkStatus LIQUID_CARVERS;



    @Shadow
    private static ChunkStatus register(
            String id,
            @Nullable ChunkStatus previous,
            int taskMargin,
            EnumSet<Heightmap.Type> heightMapTypes,
            ChunkStatus.ChunkType chunkType,
            ChunkStatus.GenerationTask task
    ){
        throw new AssertionError();
    }


    @Shadow private static final EnumSet<Heightmap.Type> PRE_CARVER_HEIGHTMAPS = EnumSet.of(Heightmap.Type.OCEAN_FLOOR_WG, Heightmap.Type.WORLD_SURFACE_WG);


    private static final ChunkStatus RIVER_POINTS = register(
            "river_points",
            EMPTY,
            0,
            PRE_CARVER_HEIGHTMAPS,
            ChunkStatus.ChunkType.PROTOCHUNK,
            (targetStatus, executor, world, chunkGenerator, structureTemplateManager, lightingProvider, function, chunks, chunk, bl) -> {
                return CompletableFuture.completedFuture(Either.left(chunk));
            }
    );

    @Shadow @Final @Mutable private static final List<ChunkStatus> DISTANCE_TO_STATUS = ImmutableList.of(
            FULL,
            FEATURES,
            LIQUID_CARVERS,
            BIOMES,
            STRUCTURE_STARTS,
            STRUCTURE_STARTS,
            STRUCTURE_STARTS,
            STRUCTURE_STARTS,
            STRUCTURE_STARTS,
            STRUCTURE_STARTS,
            STRUCTURE_STARTS,
            STRUCTURE_STARTS,
            RIVER_POINTS
    );
}
