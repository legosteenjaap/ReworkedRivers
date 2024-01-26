package me.legosteenjaap.reworkedrivers.mixin.chunk.status;


import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.legosteenjaap.reworkedrivers.NewChunkStatuses;
import me.legosteenjaap.reworkedrivers.ReworkedRivers;
import me.legosteenjaap.reworkedrivers.river.RiverBendType;
import me.legosteenjaap.reworkedrivers.river.RiverDirection;
import me.legosteenjaap.reworkedrivers.interfaces.ChunkRiverInterface;
import me.legosteenjaap.reworkedrivers.river.generation.RiverPieceGenerator;
import me.legosteenjaap.reworkedrivers.river.generation.RiverPredictor;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static me.legosteenjaap.reworkedrivers.ReworkedRivers.*;
import static net.minecraft.world.level.chunk.ChunkStatus.*;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusesMixin<T extends ChunkStatus> {

    @Shadow @Final public static EnumSet<Heightmap.Types> POST_FEATURES;
    @Shadow @Final public static ChunkStatus NOISE;

    @Shadow public abstract CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> load(ServerLevel level, StructureTemplateManager structureTemplateManager, ThreadedLevelLightEngine lightEngine, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> task, ChunkAccess loadingChunk);

    private static final int RIVER_GEN_RANGE = 16;

    @Shadow @Final public static ChunkStatus BIOMES;
    @Shadow @Final public static ChunkStatus FEATURES;

    @Shadow public abstract EnumSet<Heightmap.Types> heightmapsAfter();

    @Shadow public abstract CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> generate(Executor executor, ServerLevel level, ChunkGenerator generator, StructureTemplateManager structureTemplateManager, ThreadedLevelLightEngine lightEngine, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> task, List<ChunkAccess> neighboringChunks, boolean bl);

    @Shadow @Final public static ChunkStatus EMPTY;
    @Shadow private static final EnumSet<Heightmap.Types> PRE_FEATURES = EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG);



    @Shadow @Final public static ChunkStatus STRUCTURE_STARTS;
    @Shadow @Final public static ChunkStatus STRUCTURE_REFERENCES;
    @Shadow @Final private LoadingTask loadingTask;
    @Shadow @Final private GenerationTask generationTask;
    @Shadow @Final private String name;
    @Shadow @Final public static ChunkStatus LIQUID_CARVERS;

    private static final int[] checkLocDiagonalX = new int[]{0, 1, 0, 1, -1};
    private static final int[] checkLocDiagonalZ = new int[]{0, 0, 1, -1, 1};

    //This method inserts chunk-statuses into the vanilla generation
    @Inject(method = "register(Ljava/lang/String;Lnet/minecraft/world/level/chunk/ChunkStatus;ILjava/util/EnumSet;Lnet/minecraft/world/level/chunk/ChunkStatus$ChunkType;Lnet/minecraft/world/level/chunk/ChunkStatus$GenerationTask;Lnet/minecraft/world/level/chunk/ChunkStatus$LoadingTask;)Lnet/minecraft/world/level/chunk/ChunkStatus;", at = @At("HEAD"), cancellable = true)
    private static void modifyStructureStarts(String key, ChunkStatus parent, int taskRange, EnumSet<Heightmap.Types> heightmaps, ChunkType type, GenerationTask generationTask, LoadingTask loadingTask, CallbackInfoReturnable<ChunkStatus> cir) {
        if (key.equals("structure_starts")) {
            //At this chunk-status terrain height is estimated for a chunk
            NewChunkStatuses.RIVER_POINTS = register(
                    "river_points",
                    EMPTY,
                    0,
                    PRE_FEATURES,
                    ChunkStatus.ChunkType.PROTOCHUNK,
                    (chunkStatus, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess, bl) -> {
                        if (chunkGenerator instanceof NoiseBasedChunkGenerator) {
                            ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) chunkAccess;


                            int height = (int) (serverLevel.getChunkSource().randomState().router().depth().compute(new DensityFunction.SinglePointContext(chunkAccess.getPos().getMiddleBlockX(), 0, chunkAccess.getPos().getMiddleBlockZ())) * 128);
                            chunkRiverInterface.setRiverPoint(height);

                            /*NoiseRouter router = serverLevel.getChunkSource().randomState().router();

                            int height = serverLevel.getMaxBuildHeight();
                            while (height >= serverLevel.getMinBuildHeight()) {
                                if (router.finalDensity().compute(new DensityFunction.SinglePointContext(chunkAccess.getPos().getMiddleBlockX(), height, chunkAccess.getPos().getMiddleBlockZ())) > 0.0) {
                                    break;
                                }
                                height--;
                                height--;
                            }*/
                        }
                        return CompletableFuture.completedFuture(Either.left(chunkAccess));
                    });
            //At this chunk-status river-piece directions are determined by use of a very simple pathfinding algorithm.
            if (DEBUG_PIECES) {
                NewChunkStatuses.RIVER_PRE_GEN = register(
                        "river_pre_gen",
                        NewChunkStatuses.RIVER_POINTS,
                        RIVER_GEN_RANGE / 2,
                        PRE_FEATURES,
                        ChunkStatus.ChunkType.PROTOCHUNK,
                        (chunkStatus, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess, bl) -> {
                            ChunkPos startChunkPos = chunkAccess.getPos();
                            RandomSource random = serverLevel.getChunkSource().randomState().getOrCreateRandomFactory(new ResourceLocation(ReworkedRivers.MOD_ID)).at(startChunkPos.x, 0, startChunkPos.z);
                            WorldGenRegion worldGenRegion = new WorldGenRegion(serverLevel, list, chunkStatus, RIVER_GEN_RANGE / 2);
                            ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) chunkAccess;
                            //This code is for debugging individual river pieces
                            if (!DEBUG_CONNECTION && startChunkPos.x % 3 == 0 && startChunkPos.z % 3 == 0) {
                                RiverDirection riverDirection = RiverDirection.values()[Math.abs(startChunkPos.x / 3) % RiverDirection.values().length];
                                chunkRiverInterface.addRiverDirection(riverDirection);
                                //RiverBendType riverBendType = RiverBendType.values()[Math.abs(startChunkPos.x / 6) % RiverBendType.values().length];
                                chunkRiverInterface.setRiverBendType(DEBUG_BEND_TYPE);
                            } else if (DEBUG_CONNECTION) {
                                chunkRiverInterface.addRiverDirection(DEBUG_RIVER_DIRECTION);
                                if (DEBUG_RIVER_DIRECTION == RiverDirection.NORTH || DEBUG_RIVER_DIRECTION == RiverDirection.SOUTH) {
                                    chunkRiverInterface.setRiverBendType(startChunkPos.z % 2 == 0 ? RiverBendType.LEFT : RiverBendType.RIGHT);
                                } else {
                                    chunkRiverInterface.setRiverBendType(startChunkPos.x % 2 == 0 ? RiverBendType.LEFT : RiverBendType.RIGHT);
                                }
                            }

                            return CompletableFuture.completedFuture(Either.left(chunkAccess));
                        });
            } else {
                NewChunkStatuses.RIVER_PRE_GEN = register(
                        "river_pre_gen",
                        NewChunkStatuses.RIVER_POINTS,
                        RIVER_GEN_RANGE / 2,
                        PRE_FEATURES,
                        ChunkStatus.ChunkType.PROTOCHUNK,
                        (chunkStatus, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess, bl) -> {
                            ChunkPos startChunkPos = chunkAccess.getPos();
                            RandomSource random = serverLevel.getChunkSource().randomState().getOrCreateRandomFactory(new ResourceLocation(ReworkedRivers.MOD_ID)).at(startChunkPos.x, 0, startChunkPos.z);
                            WorldGenRegion worldGenRegion = new WorldGenRegion(serverLevel, list, chunkStatus, RIVER_GEN_RANGE / 2);
                            ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) chunkAccess;
                            if (chunkRiverInterface.getRiverPoint() < worldGenRegion.getSeaLevel() - 5 && worldGenRegion.getRandom().nextInt(9) == 0) {
                                RiverPredictor.startRiverBranch(worldGenRegion, startChunkPos, null, random.nextBoolean() ? RiverBendType.LEFT : RiverBendType.RIGHT);
                            }
                            return CompletableFuture.completedFuture(Either.left(chunkAccess));
                        });
            }
            cir.setReturnValue(Registry.register(Registry.CHUNK_STATUS, key, ChunkStatusInvoker.init(key, NewChunkStatuses.RIVER_PRE_GEN, RIVER_GEN_RANGE / 2, heightmaps, type, generationTask, loadingTask)));
        } else if (key.equals("surface")) {
            //At this chunk-status the individual blocks are generated for a river.
            NewChunkStatuses.RIVER_BLOCK_GEN = register(
                    "river_block_gen",
                    NOISE,
                    9,
                    POST_FEATURES,
                    ChunkStatus.ChunkType.PROTOCHUNK,
                    (chunkStatus, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess, bl) -> {
                        ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) chunkAccess;
                        WorldGenRegion worldGenRegion = new WorldGenRegion(serverLevel, list, chunkStatus, 1);
                        for (RiverDirection riverDirection : chunkRiverInterface.getRiverDirections()) {
                            RiverPieceGenerator.generateRiverPiece(worldGenRegion, chunkAccess, riverDirection, chunkRiverInterface.getRiverBendType(), chunkGenerator instanceof NoiseBasedChunkGenerator);
                        }
                        return CompletableFuture.completedFuture(Either.left(chunkAccess));
                    });

            cir.setReturnValue(Registry.register(Registry.CHUNK_STATUS, key, ChunkStatusInvoker.init(key, NewChunkStatuses.RIVER_BLOCK_GEN, 9, heightmaps, type, generationTask, loadingTask)));
        } else if (SURFACE != null && LIGHT == null && !key.equals("light") ) {
            cir.setReturnValue(Registry.register(Registry.CHUNK_STATUS, key, ChunkStatusInvoker.init(key, parent, 9, heightmaps, type, generationTask, loadingTask)));
        }
    }

    /**
     * This method determines at which status chunks around a completed chunk need to be within a certain distance (in chunks) to be able to fully generate
     * @return List of chunk-statuses. For example the chunk-status at position 7 is the chunk-status that a chunk needs to be 7 chunks away to make sure a chunk is able to fully generate
     */
    private static List<ChunkStatus> generateStatusByRange() {
        ArrayList<ChunkStatus> statusByRange = new ArrayList<>();
        statusByRange.add(FULL);
        statusByRange.add(FEATURES);
        statusByRange.add(LIQUID_CARVERS);
        statusByRange.add(NewChunkStatuses.RIVER_BLOCK_GEN);
        addMultipleToStatuses(statusByRange, NOISE, 1);
        addMultipleToStatuses(statusByRange, STRUCTURE_STARTS, 8);
        addMultipleToStatuses(statusByRange, NewChunkStatuses.RIVER_PRE_GEN , (RIVER_GEN_RANGE / 2));
        addMultipleToStatuses(statusByRange, NewChunkStatuses.RIVER_POINTS, RIVER_GEN_RANGE / 2);
        return statusByRange;
    }

    /**
     * Adds multiple of one status to the status by range list
     * @param statusByRange Current statuses by range
     * @param status New status
     * @param times Amount of times the status needs to be added
     */
    private static void addMultipleToStatuses (List<ChunkStatus> statusByRange, ChunkStatus status, int times) {
        for (int i = 0; i < times; i++) {
            statusByRange.add(status);
        }
    }

    //Replaces the vanilla chunk-status list
    @Shadow @Final @Mutable private static final List<ChunkStatus> STATUS_BY_RANGE = generateStatusByRange();

    @Shadow @Final @Mutable private static final IntList RANGE_BY_STATUS = Util.make(new IntArrayList(getStatusList().size()), intArrayList -> {
        int i = 0;

        for(int j = getStatusList().size() - 1; j >= 0; --j) {
            while(i + 1 < STATUS_BY_RANGE.size() && j <= ((ChunkStatus)STATUS_BY_RANGE.get(i + 1)).getIndex()) {
                ++i;
            }

            intArrayList.add(0, i);
        }

    });

    @Shadow
    private static ChunkStatus registerSimple(String key, @Nullable ChunkStatus parent, int taskRange, EnumSet<Heightmap.Types> heightmaps, ChunkType type, ChunkStatus.SimpleGenerationTask generationTask) {
        throw new AssertionError();
    }



    //REMOVES ALL GENERATION "FEATURES"
    /*@Inject(method = "method_20613", at = @At("HEAD"), cancellable = true)
    private static void removeFeatures(ChunkStatus chunkStatus, Executor executor, ServerLevel serverLevel, ChunkGenerator chunkGenerator, StructureTemplateManager structureTemplateManager, ThreadedLevelLightEngine threadedLevelLightEngine, Function function, List list, ChunkAccess chunkAccess, boolean bl, CallbackInfoReturnable<CompletableFuture> cir) {
        cir.setReturnValue(threadedLevelLightEngine.retainData(chunkAccess).thenApply(Either::left));
    }*/

    //REMOVES SURFACE GENERATION
    /*@Inject(method = "method_16569", at = @At("HEAD"), cancellable = true)
    private static void removeSurface(ChunkStatus chunkStatus, ServerLevel serverLevel, ChunkGenerator chunkGenerator, List list, ChunkAccess chunkAccess, CallbackInfo ci) {
        ci.cancel();
    }*/

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

    @Shadow @Final public static ChunkStatus SURFACE;

    @Shadow @Final public static ChunkStatus LIGHT;

}
