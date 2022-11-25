package me.legosteenjaap.reworkedrivers.mixin;


import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.legosteenjaap.reworkedrivers.NewChunkStatuses;
import me.legosteenjaap.reworkedrivers.ReworkedRivers;
import me.legosteenjaap.reworkedrivers.RiverBendType;
import me.legosteenjaap.reworkedrivers.RiverDirection;
import me.legosteenjaap.reworkedrivers.interfaces.ChunkRiverInterface;
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
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import org.apache.logging.log4j.util.PropertySource;
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

import static net.minecraft.world.level.chunk.ChunkStatus.*;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusesMixin<T extends ChunkStatus> {

    private static final boolean DEBUG_PIECES = false;
    private static final boolean DEBUG_HEIGHT = false;
    private static final boolean DEBUG_CONNECTION = true;
    private static final RiverDirection DEBUG_RIVER_DIRECTION = RiverDirection.NORTH;
    private static final RiverBendType DEBUG_BEND_TYPE = RiverBendType.RIGHT;

    @Shadow @Final public static EnumSet<Heightmap.Types> POST_FEATURES;
    @Shadow @Final public static ChunkStatus NOISE;

    @Shadow public abstract CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> load(ServerLevel level, StructureTemplateManager structureTemplateManager, ThreadedLevelLightEngine lightEngine, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> task, ChunkAccess loadingChunk);

    private static final int RIVER_GEN_RANGE = 32;

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
                            NoiseRouter router = serverLevel.getChunkSource().randomState().router();
                            int height = serverLevel.getMaxBuildHeight();
                            while (height >= serverLevel.getMinBuildHeight()) {
                                if (router.finalDensity().compute(new DensityFunction.SinglePointContext(chunkAccess.getPos().getMiddleBlockX(), height, chunkAccess.getPos().getMiddleBlockZ())) > 0.0) {
                                    break;
                                }
                                height--;
                                height--;
                            }
                            chunkRiverInterface.setRiverPoint(height);
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
                            } else {
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
                            if (chunkRiverInterface.getRiverPoint() < worldGenRegion.getSeaLevel() - 5 && worldGenRegion.getRandom().nextInt(10) == 0) {
                                startRiverBranch(worldGenRegion, startChunkPos, null, random.nextBoolean() ? RiverBendType.LEFT : RiverBendType.RIGHT);
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
                        ChunkPos chunkPos = chunkAccess.getPos();
                        for (int x = chunkPos.x - 1; x <= chunkPos.x + 1; x++) {
                            for (int z = chunkPos.z - 1; z <= chunkPos.z + 1; z++) {
                                if (!worldGenRegion.getChunk(chunkPos.x, chunkPos.z).getStatus().isOrAfter(NOISE)) throw new AssertionError();
                            }
                        }
                        try {
                            for (RiverDirection riverDirection : chunkRiverInterface.getRiverDirections()) {
                                generateRiverPiece(worldGenRegion, chunkAccess, riverDirection, chunkRiverInterface.getRiverBendType(), chunkGenerator instanceof NoiseBasedChunkGenerator);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return threadedLevelLightEngine.retainData(chunkAccess).thenApply(Either::left);
                    },
                    (chunkStatus, serverLevel, structureTemplateManager, threadedLevelLightEngine, function, chunkAccess) -> threadedLevelLightEngine.retainData(chunkAccess)
                            .thenApply(Either::left));

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
    private static void test(ChunkStatus chunkStatus, Executor executor, ServerLevel serverLevel, ChunkGenerator chunkGenerator, StructureTemplateManager structureTemplateManager, ThreadedLevelLightEngine threadedLevelLightEngine, Function function, List list, ChunkAccess chunkAccess, boolean bl, CallbackInfoReturnable<CompletableFuture> cir) {
        cir.setReturnValue(threadedLevelLightEngine.retainData(chunkAccess).thenApply(Either::left));
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

    /**
     * This method places all the blocks for a river in a specified chunk with a specified direction
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @param chunkAccess Chunk where method starts generating from
     * @param direction Direction in which the river generates
     */
    private static void generateRiverPiece(WorldGenRegion worldGenRegion, ChunkAccess chunkAccess, RiverDirection direction, RiverBendType riverBendType, boolean isNoiseBased) {
        ChunkPos chunkPos = chunkAccess.getPos();
        int startHeight = getStartHeight(worldGenRegion, chunkAccess, isNoiseBased);
        int currentYlevel = startHeight;
        ChunkPos otherChunkPos = RiverDirection.addDirectionToChunkPos(chunkPos, direction);
        int otherChunkYLevel = getStartHeight(worldGenRegion, worldGenRegion.getChunk(otherChunkPos.x, otherChunkPos.z), isNoiseBased);

        if (otherChunkYLevel > currentYlevel) return;

        //The river height is decided by checking if the terrain around the river is lower than the river
        boolean isSouthOrEast = direction == RiverDirection.SOUTH || direction == RiverDirection.EAST;
        boolean isNorthOrSouth = direction == RiverDirection.NORTH || direction == RiverDirection.SOUTH;

        //To make sure that it feels like waterfalls are carved into the terrain, terrain height is always compared to the terrain one block forward. This variable is used to determine what forward is.
        int backwardsMultiplier = isSouthOrEast ? -1 : 1;
        Integer lastOffset = null;
        for (int z = 0; z <= 17; z++) {
            BlockPos blockPos;
            int offset;
            switch (riverBendType) {
                case BOTH -> offset = (int) ((Math.sin((float) z / 16 * Math.PI * 2) * 2));
                case RIGHT -> offset = (int) ((Math.sin((float) z / 16 * Math.PI) * 6));
                case LEFT -> offset = (int) ((Math.sin(((float) z / 16 + 1) * Math.PI) * 6));
                default -> offset = 0;
            }
            if (direction == RiverDirection.SOUTH || direction == RiverDirection.WEST) offset = -offset;
            if (lastOffset == null) lastOffset = offset;
            int startX = 0;
            switch (riverBendType) {
                case LEFT -> startX = isNorthOrSouth ? 7 : 8;
                case RIGHT -> startX = isNorthOrSouth ? 8 : 7;
            }
            if (isNorthOrSouth) {
                BlockPos startPos = new BlockPos(chunkPos.getBlockX(startX), currentYlevel, chunkPos.getBlockZ(isSouthOrEast ? 7 : 8));
                blockPos = isSouthOrEast ? startPos.offset(offset, 0, z + 1) : startPos.offset(offset, 0, -z - 1);
                int height = getCurrentHeight(worldGenRegion, blockPos, true);
                if (DEBUG_HEIGHT) {
                    height = startHeight - z * 2;
                    System.out.println(height);
                }
                if (z == 16) {
                    height = otherChunkYLevel;
                }
                if (height != otherChunkYLevel && (height <= worldGenRegion.getSeaLevel() - 1 || height <= otherChunkYLevel)) {
                    height = otherChunkYLevel;
                }
                if (currentYlevel > height) {
                    setFlowingRiverBlock(worldGenRegion, startPos.getX() + lastOffset, blockPos.getZ() + backwardsMultiplier, currentYlevel, height, direction);
                    currentYlevel = height;
                    blockPos = blockPos.atY(currentYlevel);
                }
            } else {
                //switches x and z if direction is on the northwest axis
                BlockPos startPos = new BlockPos(chunkPos.getBlockX(isSouthOrEast ? 7 : 8), currentYlevel, chunkPos.getBlockZ(startX));
                blockPos = isSouthOrEast ? startPos.offset(z + 1, 0, offset) : startPos.offset(-z - 1, 0, offset);
                int height = getCurrentHeight(worldGenRegion, blockPos, false);
                if (DEBUG_HEIGHT) height = startHeight - z * 2;
                if (z == 16) {
                    height = otherChunkYLevel;
                }
                if (height != otherChunkYLevel && (height <= worldGenRegion.getSeaLevel() - 1 || height <= otherChunkYLevel)) {
                    height = otherChunkYLevel;
                }
                if (currentYlevel > height) {
                    setFlowingRiverBlock(worldGenRegion, blockPos.getX() + backwardsMultiplier, startPos.getZ() + lastOffset, currentYlevel, height, direction);
                    currentYlevel = height;
                    blockPos = blockPos.atY(currentYlevel);
                }
            }

            if (lastOffset < offset) {
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-offset + lastOffset - 1, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, -offset + lastOffset - 1));
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(1, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, 1));
                for (int i = 0; i <= offset - lastOffset; i++) {
                    setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-i, 0, backwardsMultiplier + 1) : blockPos.offset(backwardsMultiplier + 1, 0, -i));
                    setRiverBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-i, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, -i));
                    setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-i, 0, backwardsMultiplier - 1) : blockPos.offset(backwardsMultiplier - 1, 0, -i));
                }
            } else if (lastOffset > offset) {
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-1, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, -1));
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-offset + lastOffset + 1, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, -offset + lastOffset + 1));
                for (int i = 0; i >= offset - lastOffset; i--) {
                    setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-i, 0, backwardsMultiplier + 1) : blockPos.offset(backwardsMultiplier + 1, 0, -i));
                    setRiverBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-i, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, -i));
                    setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-i, 0, backwardsMultiplier - 1) : blockPos.offset(backwardsMultiplier - 1, 0, -i));
                }
            } else {
                setRiverBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(0, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, 0));
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-1, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, -1));
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(1, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, 1));
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(0, 0, backwardsMultiplier - 1) : blockPos.offset(backwardsMultiplier - 1, 0, 0));
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(0, 0, backwardsMultiplier + 1) : blockPos.offset(backwardsMultiplier + 1, 0, 0));
            }
            lastOffset = offset;
        }

    }

    /**
     * Places one water block at a position and makes sure that blocks above it are removed.
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @param blockPos Position at which the block is to be placed
     */
    private static void setRiverBlock(WorldGenRegion worldGenRegion, BlockPos blockPos) {
        if (!worldGenRegion.hasChunkAt(blockPos)) throw new AssertionError();
        ChunkAccess chunkAccess = worldGenRegion.getChunk(blockPos);
        BlockState blockState = Blocks.WATER.defaultBlockState();
        chunkAccess.setBlockState(blockPos, blockState, true);
        setSupportBlock(worldGenRegion, blockPos.below());
        for(int y = 1; y <= 3; y++) {
            if (chunkAccess.getBlockState(blockPos.atY(blockPos.getY() + y)).getBlock() != blockState.getBlock()) chunkAccess.setBlockState(blockPos.atY(blockPos.getY() + y), Blocks.AIR.defaultBlockState(), false);
        }
    }

    /**
     * Method used to place water blocks that flow downwards when a river's height drops
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @param x xPosition
     * @param z zPosition
     * @param lastYLevel The current height level from which the river flows downwards
     * @param newYLevel The new height level where the river flows towards
     */
    private static void setFlowingRiverBlock(WorldGenRegion worldGenRegion, int x, int z, int lastYLevel, int newYLevel, RiverDirection direction) {
        if (!worldGenRegion.hasChunkAt(new BlockPos(x, lastYLevel, z))) throw new AssertionError();
        if (lastYLevel < newYLevel) throw new AssertionError();
        ChunkAccess chunkAccess = worldGenRegion.getChunk(new BlockPos(x, lastYLevel, z));
        chunkAccess.setBlockState(new BlockPos(x, lastYLevel, z), Fluids.FLOWING_WATER.getFlowing(7, false).createLegacyBlock(), true);
        for (int y  = lastYLevel - 1; y > newYLevel; y--) {
            chunkAccess.setBlockState(new BlockPos(x, y, z), Fluids.FLOWING_WATER.getFlowing(8, true).createLegacyBlock(), true);
        }
    }

    /**
     * Places a simple support block for a river used to make sure that water doesn't flow on air
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @param blockPos Position at which the block is to be placed
     */
    private static void setSupportBlock(WorldGenRegion worldGenRegion, BlockPos blockPos) {
        int height = worldGenRegion.getHeight(Heightmap.Types.WORLD_SURFACE_WG, blockPos.getX(), blockPos.getZ());
        if (worldGenRegion.getBlockState(blockPos).getBlock() == Blocks.AIR && height <= blockPos.getY()) {
            for (int i = height; i <= blockPos.getY(); i++) {
                worldGenRegion.setBlock(blockPos.atY(i), Blocks.STONE.defaultBlockState(), 1);
            }
        }
    }

    /**
     * Gets the height at which a river piece in a certain chunks needs to start
     * @param worldGenRegion Used to determine sea level
     * @param chunkAccess Chunk where river piece starts
     * @return Starting height for river piece
     */
    private static int getStartHeight(WorldGenRegion worldGenRegion, ChunkAccess chunkAccess, boolean isNoiseBased) {
        if (DEBUG_HEIGHT) {
            if (((ChunkRiverInterface)chunkAccess).hasRiverDirections()) {
                return 128;
            } else {
                return 96;
            }
        }
        if (!isNoiseBased) return worldGenRegion.getHeight(Heightmap.Types.WORLD_SURFACE_WG, chunkAccess.getPos().getMiddleBlockX(), chunkAccess.getPos().getMiddleBlockZ()) - 1;
        int riverPoint = ((ChunkRiverInterface)chunkAccess).getRiverPoint() - 1;
        return Math.max(riverPoint, worldGenRegion.getSeaLevel() - 1);
    }

    /**
     * Checks the height for a certain piece of terrain. It checks an area consisting of 4 blocks (2 for water and 2 around water) and returns the lowest value.
     * @param worldGenRegion This object holds the currently loaded and usable chunks which is needed to determine height
     * @param startPos BlockPos from which the method starts scanning
     * @param isNorthOrSouth Direction in which terrain needs to be scanned
     * @return Height for terrain piece
     */
    private static int getCurrentHeight(WorldGenRegion worldGenRegion, BlockPos startPos, boolean isNorthOrSouth) {
        Integer lowestY = null;
        if (isNorthOrSouth) {
            for (int i = -1; i <= 2; i++) {
                int height = worldGenRegion.getHeight(Heightmap.Types.WORLD_SURFACE_WG, startPos.getX() + i, startPos.getZ()) - 1;
                if (lowestY == null || lowestY > height) lowestY = height;
            }
        } else {
            for (int i = -1; i <= 2; i++) {
                int height = worldGenRegion.getHeight(Heightmap.Types.WORLD_SURFACE_WG, startPos.getX(), startPos.getZ() + i) - 1;
                if (lowestY == null || lowestY > height) lowestY = height;
            }
        }
        return lowestY;
    }


    /**
     * Starts a new river branch (recursive)
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @param currentChunkPos Current position of chunk
     */
    private static boolean startRiverBranch(WorldGenRegion worldGenRegion, ChunkPos currentChunkPos, RiverDirection lastRiverDirection, RiverBendType lastRiverBendType) {
        List<RiverDirection> bestPossibleNeighbors = getBestPossibleNeighbors(currentChunkPos, worldGenRegion);
        ChunkRiverInterface currentRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(currentChunkPos.x, currentChunkPos.z);
        if (!bestPossibleNeighbors.isEmpty()) {
            int possibleNeighborAmount = 0;
            for (RiverDirection riverDirection : bestPossibleNeighbors) {
                ChunkPos newChunkPos = RiverDirection.addDirectionToChunkPos(currentChunkPos, riverDirection);
                ChunkRiverInterface newRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(newChunkPos.x, newChunkPos.z);
                if (newRiverInterface.hasRiverDirections()) continue;
                possibleNeighborAmount++;
            }
            if (possibleNeighborAmount > 1) currentRiverInterface.setSplit(true);
            int successfulBranches = 0;
            for (RiverDirection riverDirection : bestPossibleNeighbors) {
                ChunkPos newChunkPos = RiverDirection.addDirectionToChunkPos(currentChunkPos, riverDirection);
                ChunkRiverInterface newRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(newChunkPos.x, newChunkPos.z);
                if (newRiverInterface.hasRiverDirections()) continue;
                newRiverInterface.addRiverDirection(riverDirection.getOpposite());
                newRiverInterface.setRiverBendType(lastRiverBendType.getOpposite());
                if (startRiverBranch(worldGenRegion, newChunkPos, riverDirection.getOpposite(), lastRiverBendType.getOpposite())) successfulBranches++;
            }
            if (successfulBranches == 0) {
                currentRiverInterface.setSplit(false);
                removeRiverBranch(worldGenRegion, currentRiverInterface, currentChunkPos);
                return false;
            }
        } else if (currentRiverInterface.getRiverPoint() < 80) {
            removeRiverBranch(worldGenRegion, currentRiverInterface, currentChunkPos);
            return false;
        }
        return true;
    }

    /**
     * Removes river-branch if it isn't able to reach a valid source destination. A valid source is determined by estimated height this makes sure that rivers flow from places that are high up. It stops when it encounters a chunk which has multiple river-pieces flowing into the chunk.
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @param currentRiverInterface Interface which holds all the river information from a chunk
     * @param currentChunkPos Current position of chunk
     */
    private static void removeRiverBranch(WorldGenRegion worldGenRegion, ChunkRiverInterface currentRiverInterface, ChunkPos currentChunkPos) {
        while (true) {
            if (currentRiverInterface.hasSplit()) return;
            if (!currentRiverInterface.hasRiverDirections()) return;
            RiverDirection currentRiverDirection = currentRiverInterface.getRiverDirections().get(0);
            currentRiverInterface.removeRiverDirections();
            currentChunkPos = RiverDirection.addDirectionToChunkPos(currentChunkPos, currentRiverDirection);
            currentRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(currentChunkPos.x, currentChunkPos.z);
        }
    }

    /**
     * List of directions from which the river possibly can flow from (goes upwards from drain to source). This method randomly decides if a river splits or not.
     * @param chunkPos Current position of chunk
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @return List of directions from which the river possibly can flow from (goes upwards from drain to source)
     */
    private static List<RiverDirection> getBestPossibleNeighbors(ChunkPos chunkPos, WorldGenRegion worldGenRegion) {
        ChunkRiverInterface currentRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(chunkPos.x, chunkPos.z);
        int currentRiverPoint = currentRiverInterface.getRiverPoint();
        //HashMap<Integer, RiverDirection> possibleRiverDirectionMap = new HashMap<>();
        ArrayList<RiverDirection> bestPossibleNeighbors = new ArrayList<>();
        for (RiverDirection riverDirection : RiverDirection.values()) {
            ChunkPos checkingChunkPos = RiverDirection.addDirectionToChunkPos(chunkPos, riverDirection);
            if (!worldGenRegion.hasChunk(checkingChunkPos.x, checkingChunkPos.z)) {
                break;
            }
            ChunkRiverInterface checkingRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(checkingChunkPos.x, checkingChunkPos.z);
            int checkingRiverPoint = checkingRiverInterface.getRiverPoint();
            if (checkingRiverPoint >= currentRiverPoint && checkingRiverPoint > worldGenRegion.getSeaLevel() - 5 && !checkingRiverInterface.hasRiverDirections()) {
                bestPossibleNeighbors.add(riverDirection);
                //possibleRiverDirectionMap.put(checkingRiverPoint, riverDirection);
            }
        }
        if (bestPossibleNeighbors.isEmpty()) return Lists.newArrayList();

        //At the end this method randomly decides if a river splits or not.
        return worldGenRegion.getRandom().nextFloat() < 0.5 ? bestPossibleNeighbors : Lists.newArrayList(bestPossibleNeighbors.get(0));
    }

}
