package me.legosteenjaap.reworkedrivers.mixin;


import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.Registry;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.world.level.chunk.ChunkStatus.*;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusesMixin<T extends ChunkStatus> {

    @Shadow @Final private String name;
    @Shadow @Final public static ChunkStatus EMPTY;
    @Shadow @Final public static ChunkStatus LIQUID_CARVERS;


    @Inject(method = "register(Ljava/lang/String;Lnet/minecraft/world/level/chunk/ChunkStatus;ILjava/util/EnumSet;Lnet/minecraft/world/level/chunk/ChunkStatus$ChunkType;Lnet/minecraft/world/level/chunk/ChunkStatus$GenerationTask;Lnet/minecraft/world/level/chunk/ChunkStatus$LoadingTask;)Lnet/minecraft/world/level/chunk/ChunkStatus;", at = @At("HEAD"), cancellable = true)
    private static void modifyStrcutureStarts(String key, ChunkStatus parent, int taskRange, EnumSet<Heightmap.Types> heightmaps, ChunkType type, GenerationTask generationTask, LoadingTask loadingTask, CallbackInfoReturnable<ChunkStatus> cir) {
        if (key.equals("structure_starts")) cir.setReturnValue(Registry.register(Registry.CHUNK_STATUS, key, new ChunkStatus()));
    }

    @Shadow
    private static ChunkStatus register(
            String id,
            @Nullable ChunkStatus previous,
            int taskMargin,
            EnumSet<Heightmap.Types> heightMapTypes,
            ChunkStatus.ChunkType chunkType,
            ChunkStatus.GenerationTask task
    ){
        throw new AssertionError();
    }

    @Shadow private static final EnumSet<Heightmap.Types> PRE_FEATURES = EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG);


    private static final ChunkStatus RIVER_POINTS = register(
            "river_points",
            EMPTY,
            0,
            PRE_FEATURES,
            ChunkStatus.ChunkType.PROTOCHUNK,
            (targetStatus, executor, world, chunkGenerator, structureTemplateManager, lightingProvider, function, chunks, chunk, bl) -> {
                return CompletableFuture.completedFuture(Either.left(chunk));
            }
    );

    @Shadow @Final @Mutable private static final List<ChunkStatus> STATUS_BY_RANGE = ImmutableList.of(
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
