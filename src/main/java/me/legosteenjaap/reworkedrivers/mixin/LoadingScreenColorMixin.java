package me.legosteenjaap.reworkedrivers.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.legosteenjaap.reworkedrivers.NewChunkStatuses;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public class LoadingScreenColorMixin {

    @Inject(method = "method_17537", at = @At("RETURN"))
    private static void addChunkStatusColors(Object2IntOpenHashMap<ChunkStatus> object2IntOpenHashMap, CallbackInfo ci) {
        object2IntOpenHashMap.put(NewChunkStatuses.RIVER_POINTS, 864590);
        object2IntOpenHashMap.put(NewChunkStatuses.RIVER_PRE_GEN, 123123);
        object2IntOpenHashMap.put(NewChunkStatuses.RIVER_BLOCK_GEN, 578346);
    }

}
