package me.legosteenjaap.reworkedrivers.mixin;


import com.google.common.collect.Lists;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseRouter;
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

import static net.minecraft.world.level.chunk.ChunkStatus.*;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusesMixin<T extends ChunkStatus> {

    @Shadow @Final public static EnumSet<Heightmap.Types> POST_FEATURES;
    @Shadow @Final public static ChunkStatus NOISE;

    @Shadow public abstract CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> load(ServerLevel level, StructureTemplateManager structureTemplateManager, ThreadedLevelLightEngine lightEngine, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> task, ChunkAccess loadingChunk);

    private static final int RIVER_GEN_RANGE = 64;

    @Shadow @Final public static ChunkStatus BIOMES;
    @Shadow @Final public static ChunkStatus FEATURES;

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

    private static final int[] checkLocDiagonalX = new int[]{0, 1, 0, 1, -1};
    private static final int[] checkLocDiagonalZ = new int[]{0, 0, 1, -1, 1};

    //This method inserts chunk-statuses into the vanilla generation
    @Inject(method = "register(Ljava/lang/String;Lnet/minecraft/world/level/chunk/ChunkStatus;ILjava/util/EnumSet;Lnet/minecraft/world/level/chunk/ChunkStatus$ChunkType;Lnet/minecraft/world/level/chunk/ChunkStatus$GenerationTask;Lnet/minecraft/world/level/chunk/ChunkStatus$LoadingTask;)Lnet/minecraft/world/level/chunk/ChunkStatus;", at = @At("HEAD"), cancellable = true)
    private static void modifyStructureStarts(String key, ChunkStatus parent, int taskRange, EnumSet<Heightmap.Types> heightmaps, ChunkType type, GenerationTask generationTask, LoadingTask loadingTask, CallbackInfoReturnable<ChunkStatus> cir) {
        if (key.equals("structure_starts")) {
            //At this chunk-status terrain height is estimated for a chunk
            RIVER_POINTS = register(
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
                            }
                            chunkRiverInterface.setRiverPoint(height);
                        }
                        return CompletableFuture.completedFuture(Either.left(chunkAccess));
                    });
            //At this chunk-status river-piece directions are determined by use of a very simple pathfinding algorithm.
            RIVER_PRE_GEN = register(
                    "river_pre_gen",
                    RIVER_POINTS,
                    RIVER_GEN_RANGE,
                    PRE_FEATURES,
                    ChunkStatus.ChunkType.PROTOCHUNK,
                    (chunkStatus, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess, bl) -> {
                        ChunkPos startChunkPos = chunkAccess.getPos();
                        RandomSource random = serverLevel.getChunkSource().randomState().getOrCreateRandomFactory(new ResourceLocation(ReworkedRivers.MOD_ID)).at(startChunkPos.x, 0, startChunkPos.z);
                        WorldGenRegion worldGenRegion = new WorldGenRegion(serverLevel, list, chunkStatus, RIVER_GEN_RANGE);
                        ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) chunkAccess;
                        //This code is for debugging individual river pieces
                        /*if (startChunkPos.x % 3 == 0 && startChunkPos.z % 3 == 0) {
                            chunkRiverInterface.addRiverDirection(RiverDirection.NORTH);
                        }*/
                        if (chunkRiverInterface.getRiverPoint() < worldGenRegion.getSeaLevel() - 5 && worldGenRegion.getRandom().nextInt(5) == 0) {
                            startRiverBranch(worldGenRegion, startChunkPos);
                        }
                        return CompletableFuture.completedFuture(Either.left(chunkAccess));
                    });
            cir.setReturnValue(Registry.register(Registry.CHUNK_STATUS, key, ChunkStatusInvoker.init(key, RIVER_PRE_GEN, 0, heightmaps, type, generationTask, loadingTask)));
        } else if (key.equals("features")) {
            //At this chunk-status the individual blocks are generated for a river.
            RIVER_BLOCK_GEN = register(
                    "river_block_gen",
                    LIQUID_CARVERS,
                    8,
                    POST_FEATURES,
                    ChunkStatus.ChunkType.PROTOCHUNK,
                    (chunkStatus, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess, bl) -> {
                        /*ProtoChunk protoChunk = (ProtoChunk)chunkAccess;
                        protoChunk.setLightEngine(threadedLevelLightEngine);
                        */ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) chunkAccess;
                        /*if (bl || !chunkAccess.getStatus().isOrAfter(chunkStatus)) {
                            Heightmap.primeHeightmaps(
                                    chunkAccess,
                                    EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE)
                            );*/
                            WorldGenRegion worldGenRegion = new WorldGenRegion(serverLevel, list, chunkStatus, 1);
                            try {
                                for (RiverDirection riverDirection : chunkRiverInterface.getRiverDirections()) {
                                    generateRiverPiece(worldGenRegion, chunkAccess, riverDirection);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //protoChunk.setStatus(chunkStatus);
                        //}
                        return threadedLevelLightEngine.retainData(chunkAccess).thenApply(Either::left);
                    },
                    (chunkStatus, serverLevel, structureTemplateManager, threadedLevelLightEngine, function, chunkAccess) -> threadedLevelLightEngine.retainData(chunkAccess)
                            .thenApply(Either::left));

            cir.setReturnValue(Registry.register(Registry.CHUNK_STATUS, key, ChunkStatusInvoker.init(key, RIVER_BLOCK_GEN, taskRange, heightmaps, type, generationTask, loadingTask)));
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
        statusByRange.add(BIOMES);
        addMultipleToStatuses(statusByRange, STRUCTURE_STARTS, 8);
        addMultipleToStatuses(statusByRange, RIVER_POINTS, RIVER_GEN_RANGE);
        return statusByRange;
    }

    /**
     * Adds multiple of each
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

    /**
     * This method places all the blocks for a river in a specified chunk with a specified direction
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @param chunkAccess Chunk where method starts generating from
     * @param direction Direction in which the river generates
     */
    private static void generateRiverPiece(WorldGenRegion worldGenRegion, ChunkAccess chunkAccess, RiverDirection direction) {
        ChunkPos chunkPos = chunkAccess.getPos();
        int currentYlevel = getStartY(worldGenRegion, chunkAccess);
        ChunkPos otherChunkPos = RiverDirection.addDirectionToChunkPos(chunkPos, direction);
        int otherChunkYLevel = getStartY(worldGenRegion, worldGenRegion.getChunk(otherChunkPos.x, otherChunkPos.z));
        boolean upwardsRiver = false;

        //This is a shitty hack which makes sure that rivers that accidentally generate upwards don't look too weird with the help of soul sand
        //Soul sand is a type of sand in minecraft that pushes players and other entities upwards if placed underwater. Normally it doesn't generate naturally in the normal world though which makes this feel a little weird and that is why I probably need to fix it.
        if (otherChunkYLevel > currentYlevel) {
            ReworkedRivers.LOGGER.warn("River flowing upwards at: " + chunkPos.toString());
            BlockPos upwardRiverStructurePos = otherChunkPos.getBlockAt(0, 0, 0);
            for (int x = 7; x <= 8; x++) {
                for (int z = 7; z <= 8; z++) {
                    for (int y = currentYlevel; otherChunkYLevel >= y; y++) {
                        setRiverBlock(worldGenRegion, upwardRiverStructurePos.offset(x, y, z));
                    }
                }
            }
            upwardsRiver = true;
        }

        //Makes sure that the start of the current river part is filled with water
        for (int x = 7; x <= 8; x++) {
            for (int z = 7; z <= 8; z++) {
                setRiverBlock(worldGenRegion, new BlockPos(chunkPos.getBlockX(x), currentYlevel, chunkPos.getBlockZ(z)));
            }
        }

        if (direction.isDiagonal()) {
            //Code for diagonal river pieces (currently not used)
            for (int i = 0; i <= 15; i++) {
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
                blockPos = new BlockPos(chunkPos.getBlockX(7 + (xOffset - i) * multiplierX), currentYlevel, chunkPos.getBlockZ(7 + (zOffset - i) * multiplierZ));
                int height = getDiagonalY(worldGenRegion, blockPos, multiplierX, multiplierZ);
                if (currentYlevel > height && height >= worldGenRegion.getSeaLevel() - 1) {
                    currentYlevel = height;
                    blockPos = blockPos.atY(currentYlevel);
                }
                setRiverBlock(worldGenRegion, blockPos);
                setRiverBlock(worldGenRegion, blockPos.offset(multiplierX, 0, 0));
                setRiverBlock(worldGenRegion, blockPos.offset(0, 0, multiplierZ));
                if (chunkAccess.getBlockState(blockPos.offset(multiplierX * 2, 0, 0)) == Blocks.AIR.defaultBlockState()) {
                    setSupportBlock(worldGenRegion, blockPos.offset(multiplierX * 2, 0, 0));
                } else if (chunkAccess.getBlockState(blockPos.offset(0, 0, multiplierZ * 2)) == Blocks.AIR.defaultBlockState()) {
                    setSupportBlock(worldGenRegion, blockPos.offset(0, 0, multiplierZ * 2));
                }
            }
        } else {
            //Code for non diagonal river pieces

            //The river height is decided by checking if the terrain around the river is lower than the river
            boolean isSouthOrEast = direction == RiverDirection.SOUTH || direction == RiverDirection.EAST;
            boolean isNorthOrSouth = direction == RiverDirection.NORTH || direction == RiverDirection.SOUTH;

            //To make sure that it feels like waterfalls are carved into the terrain, terrain height is always compared to the terrain one block forward. This variable is used to determine what forward is.
            int backwardsMultiplier = isSouthOrEast ? -1 : 1;

            for (int z = 0; z <= 15; z++) {
                BlockPos blockPos;

                if (isNorthOrSouth) {
                    BlockPos startPos = new BlockPos(chunkPos.getBlockX(7), currentYlevel, chunkPos.getBlockZ(isSouthOrEast ? 8 : 7));
                    blockPos = isSouthOrEast ? startPos.offset(0, 0, z + 1) : startPos.offset(0, 0, -z - 1);
                    int height = getNonDiagonalY(worldGenRegion, blockPos, true);
                    if (z == 15) {
                        height = otherChunkYLevel;
                    }
                    if (currentYlevel > height && height >= worldGenRegion.getSeaLevel() - 1 && height >= otherChunkYLevel) {
                        setFlowingRiverBlock(worldGenRegion, blockPos.getX(), blockPos.getZ() + backwardsMultiplier, currentYlevel, height, direction);
                        setFlowingRiverBlock(worldGenRegion, blockPos.getX() + 1, blockPos.getZ() + backwardsMultiplier, currentYlevel, height, direction);
                        currentYlevel = height;
                        blockPos = blockPos.atY(currentYlevel);
                    }
                } else {
                    //switches x and z if direction is on the northwest axis
                    BlockPos startPos = new BlockPos(chunkPos.getBlockX(isSouthOrEast ? 8 : 7), currentYlevel, chunkPos.getBlockZ(7));
                    blockPos = isSouthOrEast ? startPos.offset(z + 1, 0, 0) : startPos.offset(-z - 1, 0, 0);
                    int height = getNonDiagonalY(worldGenRegion, blockPos, false);
                    if (z == 15) {
                        height = otherChunkYLevel;
                    }
                    if (currentYlevel > height && height >= worldGenRegion.getSeaLevel() - 1 && height >= otherChunkYLevel) {
                        setFlowingRiverBlock(worldGenRegion, blockPos.getX() + backwardsMultiplier, blockPos.getZ(), currentYlevel, height, direction);
                        setFlowingRiverBlock(worldGenRegion, blockPos.getX() + backwardsMultiplier, blockPos.getZ() + 1, currentYlevel, height, direction);
                        currentYlevel = height;
                        blockPos = blockPos.atY(currentYlevel);
                    }
                }
                setRiverBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(0, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, 0));
                setRiverBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(1, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, 1));
            }
        }

        //Continuation of shitty hack
        if (upwardsRiver) {
            BlockPos chunkOriginPos = otherChunkPos.getBlockAt(0, 0, 0);
            for (int x = 6; x <= 9; x++) {
                for (int y = currentYlevel; otherChunkYLevel >= y; y++) {
                    for (int z = 6; z <= 9; z++) {
                        setSupportBlock(worldGenRegion, chunkOriginPos.offset(x, y, z));
                    }
                }
            }
            for (int x = 7; x <= 8; x++) {
                for (int z = 7; z <= 8; z++) {
                    for (int y = currentYlevel; otherChunkYLevel >= y; y++) {
                        setRiverBlock(worldGenRegion, chunkOriginPos.offset(x, y, z));
                    }
                }
            }
            for (int x = 7; x <= 8; x++) {
                for (int z = 7; z <= 8; z++) {
                    setSoulSandBlock(worldGenRegion, chunkOriginPos.offset(x, currentYlevel - 1, z));
                }
            }
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
        chunkAccess.setBlockState(blockPos, Blocks.WATER.defaultBlockState(), true);
        if (chunkAccess.getBlockState(blockPos.below()) == Blocks.AIR.defaultBlockState()) chunkAccess.setBlockState(blockPos.below(), Blocks.DIRT.defaultBlockState(), false);
        for(int y = 1; y <= 5; y++) {
            if (chunkAccess.getBlockState(blockPos.atY(blockPos.getY() + y)).getBlock() != Blocks.WATER) chunkAccess.setBlockState(blockPos.atY(blockPos.getY() + y), Blocks.AIR.defaultBlockState(), false);

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
        if (worldGenRegion.getBlockState(blockPos).getBlock() != Blocks.WATER) worldGenRegion.setBlock(blockPos, Blocks.DIRT.defaultBlockState(), 1);
    }

    /**
     * Places a soul-sand block at a position. Currently, doesn't activate water bubbles which is strange.
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @param blockPos Position at which the block is to be placed
     */
    private static void setSoulSandBlock(WorldGenRegion worldGenRegion, BlockPos blockPos) {
        if (worldGenRegion.getBlockState(blockPos).getBlock() != Blocks.WATER) worldGenRegion.setBlock(blockPos, Blocks.SOUL_SAND.defaultBlockState(), 2);
    }

    /**
     * Gets the height at which a river piece in a certain chunks needs to start
     * @param worldGenRegion Used to determine sea level
     * @param chunkAccess Chunk where river piece starts
     * @return Starting height for river piece
     */
    private static int getStartY(WorldGenRegion worldGenRegion, ChunkAccess chunkAccess) {
        Integer lowestY = null;

        for (int x = 6; x <= 9; x++) {
            for (int z = 6; z <= 9; z++) {
                int height = chunkAccess.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1; //Subtracts one block to make sure the river is carved deeper inside of the terrain

                //If another part of the river is already generated the height should not be carved deeper by one block
                if (chunkAccess.getBlockState(new BlockPos(x, height, z)).getBlock() == Blocks.WATER) height = height + 1;
                if ((lowestY == null) || height < lowestY) lowestY = height;
            }
        }
        if (lowestY < worldGenRegion.getSeaLevel() - 1) lowestY = worldGenRegion.getSeaLevel() - 1;
        return lowestY;
    }

    /**
     * Checks the height for a certain piece of terrain. It checks an area consisting of 4 blocks (2 for water and 2 around water) and returns the lowest value.
     * @param worldGenRegion This object holds the currently loaded and usable chunks which is needed to determine height
     * @param startPos BlockPos from which the method starts scanning
     * @param isNorthOrSouth Direction in which terrain needs to be scanned
     * @return Height for terrain piece
     */
    private static int getNonDiagonalY(WorldGenRegion worldGenRegion, BlockPos startPos, boolean isNorthOrSouth) {
        Integer lowestY = null;
        if (isNorthOrSouth) {
            for (int i = 0; i <= 4; i++) {
                int height = worldGenRegion.getHeight(Heightmap.Types.WORLD_SURFACE_WG, startPos.getX() + i, startPos.getZ()) - 2;
                if (lowestY == null || lowestY > height) lowestY = height;
            }
        } else {
            for (int i = 0; i <= 4; i++) {
                int height = worldGenRegion.getHeight(Heightmap.Types.WORLD_SURFACE_WG, startPos.getX(), startPos.getZ() + i) - 2;
                if (lowestY == null || lowestY > height) lowestY = height;
            }
        }
        return lowestY;
    }

    /**
     * Diagonals are currenly not supported (old code)
     * @param worldGenRegion
     * @param startPos
     * @param multiplierX
     * @param multiplierZ
     * @return
     */
    private static int getDiagonalY(WorldGenRegion worldGenRegion, BlockPos startPos, int multiplierX, int multiplierZ) {
        Integer lowestY = null;
        for (int i = 0; i <=4; i++) {
            int height = worldGenRegion.getHeight(Heightmap.Types.WORLD_SURFACE_WG, startPos.getX() + checkLocDiagonalX[i] * multiplierX, startPos.getZ() + checkLocDiagonalZ[i] * multiplierZ) - 2;
            if (lowestY == null || lowestY > height) lowestY = height;
        }
        return lowestY;
    }


    /**
     * Starts a new river branch (recursive)
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @param currentChunkPos Current position of chunk
     */
    private static boolean startRiverBranch(WorldGenRegion worldGenRegion, ChunkPos currentChunkPos) {
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
            if (possibleNeighborAmount > 1) currentRiverInterface.setSplit();
            int successfulBranches = 0;
            for (RiverDirection riverDirection : bestPossibleNeighbors) {
                ChunkPos newChunkPos = RiverDirection.addDirectionToChunkPos(currentChunkPos, riverDirection);
                ChunkRiverInterface newRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(newChunkPos.x, newChunkPos.z);
                if (newRiverInterface.hasRiverDirections()) continue;
                newRiverInterface.addRiverDirection(riverDirection.getOpposite());
                if (startRiverBranch(worldGenRegion, newChunkPos)) successfulBranches++;
            }
            if (successfulBranches == 0) removeRiverBranch(worldGenRegion, currentRiverInterface, currentChunkPos);
        } else if (currentRiverInterface.getRiverPoint() < 100) {
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
        double currentRiverPoint = currentRiverInterface.getRiverPoint();
        HashMap<Double, RiverDirection> possibleRiverDirectionMap = new HashMap<>();
        for (RiverDirection riverDirection : Arrays.stream(RiverDirection.values()).filter(RiverDirection::isStraight).toList()) {
            ChunkPos checkingChunkPos = RiverDirection.addDirectionToChunkPos(chunkPos, riverDirection);
            if (!worldGenRegion.hasChunk(checkingChunkPos.x, checkingChunkPos.z)) break;
            ChunkRiverInterface checkingRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(checkingChunkPos.x, checkingChunkPos.z);
            double checkingRiverPoint = checkingRiverInterface.getRiverPoint();
            if (checkingRiverPoint > currentRiverPoint && checkingRiverPoint > worldGenRegion.getSeaLevel() && !checkingRiverInterface.hasRiverDirections()) {
                possibleRiverDirectionMap.put(checkingRiverPoint, riverDirection);
            }
        }
        //Sorts the possible river directions from best to worst.
        ArrayList<Double> sortedMapKeys = Lists.newArrayList(possibleRiverDirectionMap.keySet().stream().toList());
        Collections.sort(sortedMapKeys);
        Collections.reverse(sortedMapKeys);
        ArrayList<RiverDirection> bestPossibleNeighbors = new ArrayList<>();
        for (Double riverDirectionMapKey : sortedMapKeys) {
            bestPossibleNeighbors.add(possibleRiverDirectionMap.get(riverDirectionMapKey));
        }
        if (bestPossibleNeighbors.isEmpty()) return Lists.newArrayList();

        //At the end this method randomly decides if a river splits or not.
        return (worldGenRegion.getRandom().nextFloat() < 0.30 && currentRiverPoint > 0.5f)? bestPossibleNeighbors : Lists.newArrayList(bestPossibleNeighbors.get(bestPossibleNeighbors.size() - 1));
    }

}
