package me.legosteenjaap.reworkedrivers.mixin;

import me.legosteenjaap.reworkedrivers.interfaces.ChunkRiverInterface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {

    @Shadow protected abstract Level getLevel();

    @Shadow @Final private Minecraft minecraft;

    @Shadow @Nullable protected abstract ServerLevel getServerLevel();

    //This method injects extra data into the debug screen of minecraft which is opened with the f3 key.
    @Inject(method = "getGameInformation", at = @At(value = "RETURN"))
    protected void getGameInformation(CallbackInfoReturnable<List<String>> cir) {
        BlockPos blockPos = this.minecraft.getCameraEntity().blockPosition();
        ChunkAccess chunkAccess = getLevel().getChunk(blockPos);
        ChunkRiverInterface chunkRiverInterface = (ChunkRiverInterface) chunkAccess;
        cir.getReturnValue().add("RiverPoint: " + ((ChunkRiverInterface)chunkAccess).getRiverPoint());
        NoiseRouter router = getServerLevel().getChunkSource().randomState().router();
        double riverPoint = router.finalDensity().compute(new DensityFunction.SinglePointContext(minecraft.player.getBlockX(), minecraft.player.getBlockY(), minecraft.player.getBlockZ()));
        cir.getReturnValue().add("Final Density: " + riverPoint);
        cir.getReturnValue().add("River-pieces: " + chunkRiverInterface.getRiverDirections().toString());
    }

}
