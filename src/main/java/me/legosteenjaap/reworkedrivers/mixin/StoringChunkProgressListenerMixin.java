package me.legosteenjaap.reworkedrivers.mixin;

import me.legosteenjaap.reworkedrivers.ReworkedRivers;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StoringChunkProgressListener.class)
public class StoringChunkProgressListenerMixin {

    Long startTime;

    @Inject(method = "start", at = @At("HEAD"))
    public void start(CallbackInfo ci) {
        startTime = System.currentTimeMillis();
    }

    @Inject(method = "stop", at = @At("HEAD"))
    public void stop(CallbackInfo ci) {
        ReworkedRivers.LOGGER.info("World generation took: " + String.valueOf(System.currentTimeMillis() - startTime) + " ms");
        startTime = 0L;
    }

}
