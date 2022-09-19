package me.legosteenjaap.reworkedrivers.mixin;

import me.legosteenjaap.reworkedrivers.RiverDirection;
import me.legosteenjaap.reworkedrivers.interfaces.ChunkRiverInterface;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
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

import java.util.List;
import java.util.stream.Collectors;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {

    @Inject(method = "write", at = @At("RETURN"), cancellable = true)
    private static void write(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<CompoundTag> cir) {
        ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) chunk;
        CompoundTag compoundTag = cir.getReturnValue();
        compoundTag.putDouble("riverPoint", chunkRiverInterface.getRiverPoint());
        ListTag riverDirections = new ListTag();
        riverDirections.addAll(chunkRiverInterface.getRiverDirections().stream().map((riverDirection) -> {
            return StringTag.valueOf(riverDirection.toString());
        }).toList());
        compoundTag.put("riverDirections", riverDirections);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void read(ServerLevel lvel, PoiManager poiManager, ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<ProtoChunk> cir) {
        ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) cir.getReturnValue();
        if (tag.contains("riverPoint")) chunkRiverInterface.setRiverPoint(tag.getDouble("riverPoint"));
        if (tag.contains("riverDirections")) {
            ListTag riverDirections = tag.getList("riverDirections", 8);
            for (Tag riverDirection : riverDirections) {
                chunkRiverInterface.addRiverDirection(RiverDirection.valueOf(riverDirection.getAsString()));
            }
        }
    }

}
