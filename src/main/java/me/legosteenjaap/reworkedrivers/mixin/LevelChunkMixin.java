package me.legosteenjaap.reworkedrivers.mixin;

import me.legosteenjaap.reworkedrivers.interfaces.ChunkRiverInterface;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public class LevelChunkMixin {

    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V", at = @At("RETURN"))
    private void init(ServerLevel serverLevel, ProtoChunk protoChunk, LevelChunk.PostLoadProcessor postLoadProcessor, CallbackInfo ci) {
        ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface)this;
        ChunkRiverInterface protoChunkRiverInterface = (ChunkRiverInterface)protoChunk;
        chunkRiverInterface.setRiverPoint(protoChunkRiverInterface.getRiverPoint());
    }
}
