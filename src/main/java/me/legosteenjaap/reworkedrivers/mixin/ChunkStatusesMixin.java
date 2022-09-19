package me.legosteenjaap.reworkedrivers.mixin;


import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import me.legosteenjaap.reworkedrivers.ReworkedRivers;
import me.legosteenjaap.reworkedrivers.RiverDirection;
import me.legosteenjaap.reworkedrivers.interfaces.ChunkRiverInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
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
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static net.minecraft.world.level.chunk.ChunkStatus.*;
import static org.quiltmc.loader.impl.QuiltLoaderImpl.MOD_ID;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusesMixin<T extends ChunkStatus> {


    @Shadow public abstract EnumSet<Heightmap.Types> heightmapsAfter();

    @Shadow public abstract CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> generate(Executor executor, ServerLevel level, ChunkGenerator generator, StructureTemplateManager structureTemplateManager, ThreadedLevelLightEngine lightEngine, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> task, List<ChunkAccess> neighboringChunks, boolean bl);

    @Shadow @Final public static ChunkStatus EMPTY;
    @Shadow private static final EnumSet<Heightmap.Types> PRE_FEATURES = EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG);

    private static ChunkStatus RIVER_POINTS = EMPTY;
    private static ChunkStatus RIVER_PRE_GEN = EMPTY;
    private static ChunkStatus RIVER_BLOCK_GEN = EMPTY;

    @Shadow @Final public static ChunkStatus STRUCTURE_STARTS;
    @Shadow @Final public static ChunkStatus STRUCTURE_REFERENCES;
    @Shadow @Final private LoadingTask loadingTask;
    @Shadow @Final private GenerationTask generationTask;
    @Shadow @Final private String name;
    @Shadow @Final public static ChunkStatus LIQUID_CARVERS;

    @Inject(method = "register(Ljava/lang/String;Lnet/minecraft/world/level/chunk/ChunkStatus;ILjava/util/EnumSet;Lnet/minecraft/world/level/chunk/ChunkStatus$ChunkType;Lnet/minecraft/world/level/chunk/ChunkStatus$GenerationTask;Lnet/minecraft/world/level/chunk/ChunkStatus$LoadingTask;)Lnet/minecraft/world/level/chunk/ChunkStatus;", at = @At("HEAD"), cancellable = true)
    private static void modifyStructureStarts(String key, ChunkStatus parent, int taskRange, EnumSet<Heightmap.Types> heightmaps, ChunkType type, GenerationTask generationTask, LoadingTask loadingTask, CallbackInfoReturnable<ChunkStatus> cir) {

        if (key.equals("structure_starts")) {
            RIVER_POINTS = register(
                    "river_points",
                    EMPTY,
                    0,
                    PRE_FEATURES,
                    ChunkStatus.ChunkType.PROTOCHUNK,
                    (chunkStatus, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess, bl) -> {
                        ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) chunkAccess;
                        double riverPoint = serverLevel.getChunkSource().randomState().sampler().depth().compute(new DensityFunction.SinglePointContext(chunkAccess.getPos().getMiddleBlockX(), 0, chunkAccess.getPos().getMiddleBlockZ()));
                        chunkRiverInterface.setRiverPoint(riverPoint);
                        return CompletableFuture.completedFuture(Either.left(chunkAccess));
                    });
            RIVER_PRE_GEN = register(
                    "river_pre_gen",
                    RIVER_POINTS,
                    16,
                    PRE_FEATURES,
                    ChunkStatus.ChunkType.PROTOCHUNK,
                    (chunkStatus, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess, bl) -> {
                        ChunkPos startChunkPos = chunkAccess.getPos();
                        RandomSource random = serverLevel.getChunkSource().randomState().getOrCreateRandomFactory(new ResourceLocation(ReworkedRivers.MOD_ID)).at(startChunkPos.x, 0, startChunkPos.z);
                        WorldGenRegion worldGenRegion = new WorldGenRegion(serverLevel, list, chunkStatus, 16);
                        ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) chunkAccess;
                        if (chunkRiverInterface.getRiverPoint() < 0.4f && random.nextInt(10) == 0) {
                            ChunkPos currentChunkPos = startChunkPos;
                            ChunkRiverInterface currentRiverInterface = chunkRiverInterface;
                            while (true) {
                                Optional<RiverDirection> bestPossibleNeighbor = getBestPossibleNeighbor(currentChunkPos, worldGenRegion);
                                if (bestPossibleNeighbor.isPresent()) {
                                    RiverDirection riverDirection = bestPossibleNeighbor.get();
                                    currentRiverInterface.addRiverDirection(riverDirection);
                                    currentChunkPos = RiverDirection.addDirectionToChunkPos(currentChunkPos, riverDirection);
                                    currentRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(currentChunkPos.x, currentChunkPos.z);
                                    currentRiverInterface.addRiverDirection(riverDirection.getOpposite());
                                } else {
                                    break;
                                }
                            }
                        }
                        return CompletableFuture.completedFuture(Either.left(chunkAccess));
                    });
            cir.setReturnValue(Registry.register(Registry.CHUNK_STATUS, key, ChunkStatusInvoker.init(key, RIVER_PRE_GEN, 16, heightmaps, type, generationTask, loadingTask)));
        } else if (key.equals("surface")) {
            RIVER_BLOCK_GEN = register(
                    "river_block_gen",
                    NOISE,
                    0,
                    PRE_FEATURES,
                    ChunkStatus.ChunkType.PROTOCHUNK,
                    (chunkStatus, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess, bl) -> {
                        ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) chunkAccess;
                        for (RiverDirection riverDirection : chunkRiverInterface.getRiverDirections()) {
                            generateRiverBlocks(chunkAccess, riverDirection);
                        }
                        return CompletableFuture.completedFuture(Either.left(chunkAccess));
                    });
            cir.setReturnValue(Registry.register(Registry.CHUNK_STATUS, key, ChunkStatusInvoker.init(key, RIVER_BLOCK_GEN, taskRange, heightmaps, type, generationTask, loadingTask)));
        }
    }

    @Shadow
    private static ChunkStatus registerSimple(String key, @Nullable ChunkStatus parent, int taskRange, EnumSet<Heightmap.Types> heightmaps, ChunkType type, ChunkStatus.SimpleGenerationTask generationTask) {
        throw new AssertionError();
    }

    @Shadow
    private static ChunkStatus register(
            String key,
            @Nullable ChunkStatus parent,
            int taskRange,
            EnumSet<Heightmap.Types> heightmapTypes,
            ChunkStatus.ChunkType type,
            ChunkStatus.GenerationTask generationTask,
            ChunkStatus.LoadingTask loadingTask
    ) {
        throw new AssertionError();
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

    private static void generateRiverBlocks(ChunkAccess chunkAccess, RiverDirection direction) {
        int lowestY = 0;

        ChunkPos chunkPos = chunkAccess.getPos();

        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                int height = chunkAccess.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                if ((x == 0 && z == 0) || height < lowestY) lowestY = height;
            }
        }

        if (lowestY < 62) lowestY = 62;

        if (direction.isDiagonal()) {
            for (int i = 0; i <= 7; i++) {
                int multiplierX = 1;
                int multiplierZ = 1;
                switch (direction) {
                    case NORTHEAST -> multiplierX = -1;
                    case SOUTHEAST -> {
                        multiplierX = -1;
                        multiplierZ = -1;
                    }
                    case SOUTHWEST -> multiplierZ = -1;
                }
                BlockPos blockPos;
                int xOffset = 0;
                int zOffset = 0;
                if (multiplierX == -1) xOffset = -1;
                if (multiplierZ == -1) zOffset = -1;
                blockPos = new BlockPos(chunkPos.getBlockX(7 + (xOffset - i) * multiplierX), lowestY, chunkPos.getBlockZ(7 + (zOffset - i) * multiplierZ));
                setRiverBlock(chunkAccess, blockPos);
                setRiverBlock(chunkAccess, blockPos.offset(multiplierX, 0, 0));
                setRiverBlock(chunkAccess, blockPos.offset(2 * multiplierX, 0, 0));
                setRiverBlock(chunkAccess, blockPos.offset(0, 0, multiplierZ));
                setRiverBlock(chunkAccess, blockPos.offset(0, 0, 2 * multiplierZ));
            }
        } else {
            boolean alternateZ = direction == RiverDirection.SOUTH || direction == RiverDirection.EAST;
            for (int x = 7; x <= 8; x++) {
                for (int z = alternateZ ? 15 : 7; z >= (alternateZ ? 8 : 0); z--) {
                    BlockPos blockPos;
                    if (direction == RiverDirection.NORTH || direction == RiverDirection.SOUTH) {
                        blockPos = new BlockPos(chunkPos.getBlockX(x), lowestY, chunkPos.getBlockZ(z));
                    } else {
                        //switches x and z if direction is on the northwest axis
                        blockPos = new BlockPos(chunkPos.getBlockZ(z), lowestY, chunkPos.getBlockX(x));
                    }
                    setRiverBlock(chunkAccess, blockPos);
                }
            }
        }
    }

    private static Optional<RiverDirection> getBestPossibleNeighbor(ChunkPos chunkPos, WorldGenRegion worldGenRegion) {
        Optional<RiverDirection> bestPossibleNeighbor = Optional.empty();
        ChunkRiverInterface currentRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(chunkPos.x, chunkPos.z);
        double highestRiverPoint = 0;
        for (RiverDirection riverDirection : RiverDirection.values()) {
            ChunkPos checkingChunkPos = RiverDirection.addDirectionToChunkPos(chunkPos, riverDirection);
            if (!worldGenRegion.hasChunk(checkingChunkPos.x, checkingChunkPos.z)) return Optional.empty();
            ChunkRiverInterface checkingRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(checkingChunkPos.x, checkingChunkPos.z);
            double checkingRiverPoint = checkingRiverInterface.getRiverPoint();
            if (checkingRiverPoint > highestRiverPoint ) {
                if (currentRiverInterface.getRiverPoint() < checkingRiverPoint) {
                    bestPossibleNeighbor = Optional.of(riverDirection);
                    highestRiverPoint = checkingRiverPoint;
                }
            }
        }
        return bestPossibleNeighbor;
    }

    private static void setRiverBlock(ChunkAccess chunkAccess, BlockPos blockPos) {
        chunkAccess.setBlockState(blockPos, Blocks.WATER.defaultBlockState(), true);
        for(int y = 1; y <= 32; y++) {
            chunkAccess.setBlockState(blockPos.atY(blockPos.getY() + y), Blocks.AIR.defaultBlockState(), false);
        }
    }

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
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS,
            RIVER_POINTS
            );
}
