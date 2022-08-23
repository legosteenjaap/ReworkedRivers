package me.legosteenjaap.reworkedrivers.mixin;

import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.EnumSet;

@Mixin(ChunkStatus.class)
public interface ChunkStatusInvoker {

    @Invoker("<init>(Ljava/lang/String;Lnet/minecraft/world/level/chunk/ChunkStatus;ILjava/util/EnumSet;Lnet/minecraft/world/level/chunk/ChunkStatus$ChunkType;Lnet/minecraft/world/level/chunk/ChunkStatus$GenerationTask;Lnet/minecraft/world/level/chunk/ChunkStatus$LoadingTask;)V")
    public static void init(String string, @Nullable ChunkStatus chunkStatus, int i, EnumSet<Heightmap.Types> enumSet, ChunkStatus.ChunkType chunkType, ChunkStatus.GenerationTask generationTask, ChunkStatus.LoadingTask loadingTask) {
        throw new AssertionError();
    }

}
