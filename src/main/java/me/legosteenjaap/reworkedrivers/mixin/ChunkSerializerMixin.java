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
        writeRiverDirections(compoundTag, chunkRiverInterface.getRiverUpDirections(), "riverUpDirections");
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void read(ServerLevel lvel, PoiManager poiManager, ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<ProtoChunk> cir) {
        ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) cir.getReturnValue();
        if (tag.contains("riverPoint")) chunkRiverInterface.setRiverPoint(tag.getDouble("riverPoint"));
        readRiverUpDirections(tag, chunkRiverInterface);
    }

    private static void writeRiverDirections(CompoundTag compoundTag, List<RiverDirection> riverDirections, String key) {
        ListTag riverDirectionsTag = new ListTag();
        riverDirectionsTag.addAll(riverDirections.stream().map((riverDirection) -> {
            return StringTag.valueOf(riverDirection.toString());
        }).toList());
        compoundTag.put(key, riverDirectionsTag);
    }

    private static void readRiverUpDirections(CompoundTag compoundTag, ChunkRiverInterface chunkRiverInterface) {
        if (compoundTag.contains("riverUpDirections")) {
            ListTag riverDirections = compoundTag.getList("riverUpDirections", 8);
            for (Tag riverDirection : riverDirections) {
                chunkRiverInterface.addRiverUpDirection(RiverDirection.valueOf(riverDirection.getAsString()));
            }
        }
    }

}
