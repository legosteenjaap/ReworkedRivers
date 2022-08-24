package me.legosteenjaap.reworkedrivers.mixin;

import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.EnumSet;

@Mixin(ChunkStatus.class)
public interface ChunkStatusInvoker {

    @Invoker("<init>")
    public static ChunkStatus init(String string, @Nullable ChunkStatus chunkStatus, int i, EnumSet<Heightmap.Types> enumSet, ChunkStatus.ChunkType chunkType, ChunkStatus.GenerationTask generationTask, ChunkStatus.LoadingTask loadingTask) {
        throw new AssertionError();
    }

}
