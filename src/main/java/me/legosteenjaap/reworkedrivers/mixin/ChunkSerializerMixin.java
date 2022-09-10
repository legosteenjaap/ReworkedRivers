package me.legosteenjaap.reworkedrivers.mixin;

import me.legosteenjaap.reworkedrivers.interfaces.ChunkRiverInterface;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {

    @Inject(method = "write", at = @At("RETURN"), cancellable = true)
    private static void write(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<CompoundTag> cir) {
        ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) chunk;
        cir.getReturnValue().putDouble("riverPoint", chunkRiverInterface.getRiverPoint());
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void read(ServerLevel lvel, PoiManager poiManager, ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<ProtoChunk> cir) {
        ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) cir.getReturnValue();
        if (tag.contains("riverPoint")) chunkRiverInterface.setRiverPoint(tag.getDouble("riverPoint"));
    }

}
