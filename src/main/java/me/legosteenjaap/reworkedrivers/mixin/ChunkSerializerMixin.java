package me.legosteenjaap.reworkedrivers.mixin;

import me.legosteenjaap.reworkedrivers.RiverBendType;
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

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {


    //Writes all the river data to disk
    @Inject(method = "write", at = @At("RETURN"), cancellable = true)
    private static void write(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<CompoundTag> cir) {
        ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) chunk;
        CompoundTag compoundTag = cir.getReturnValue();
        compoundTag.putInt("riverPoint", chunkRiverInterface.getRiverPoint());
        if (chunkRiverInterface.getRiverBendType() != null) compoundTag.putString("riverBendType", chunkRiverInterface.getRiverBendType().toString());
        compoundTag.putBoolean("hasSplit", chunkRiverInterface.hasSplit());
        writeRiverDirections(compoundTag, chunkRiverInterface.getRiverDirections(), "riverDirections");
    }

    //Reads all the river data from disk
    @Inject(method = "read", at = @At("RETURN"))
    private static void read(ServerLevel lvel, PoiManager poiManager, ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<ProtoChunk> cir) {
        ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) cir.getReturnValue();
        if (tag.contains("riverPoint")) chunkRiverInterface.setRiverPoint(tag.getInt("riverPoint"));
        if (tag.contains("riverBendType")) chunkRiverInterface.setRiverBendType(RiverBendType.valueOf(tag.getString("riverBendType")));
        if (tag.contains("hasSplit")) chunkRiverInterface.setSplit(tag.getBoolean("hasSplit"));
        readRiverDirections(tag, chunkRiverInterface);
    }

    //Writes river directions to disk (this is extra difficult because you need to save multiple river directions and not one)
    private static void writeRiverDirections(CompoundTag compoundTag, List<RiverDirection> riverDirections, String key) {
        ListTag riverDirectionsTag = new ListTag();
        riverDirectionsTag.addAll(riverDirections.stream().map((riverDirection) -> {
            return StringTag.valueOf(riverDirection.toString());
        }).toList());
        compoundTag.put(key, riverDirectionsTag);
    }

    //Reads river directions from disk
    private static void readRiverDirections(CompoundTag compoundTag, ChunkRiverInterface chunkRiverInterface) {
        if (compoundTag.contains("riverDirections")) {
            ListTag riverDirections = compoundTag.getList("riverDirections", 8);
            for (Tag riverDirection : riverDirections) {
                chunkRiverInterface.addRiverDirection(RiverDirection.valueOf(riverDirection.getAsString()));
            }
        }
    }

}
