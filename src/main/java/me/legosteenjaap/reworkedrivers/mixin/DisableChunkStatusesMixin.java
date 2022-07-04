package me.legosteenjaap.reworkedrivers.mixin;


import com.google.common.collect.ImmutableList;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;
import java.util.List;

import static net.minecraft.world.chunk.ChunkStatus.*;

@Mixin(ChunkStatus.class)
public abstract class DisableChunkStatusesMixin <T extends ChunkStatus> {

    @Shadow @Final public static ChunkStatus LIQUID_CARVERS;

    @Redirect(method = "register(Ljava/lang/String;Lnet/minecraft/world/chunk/ChunkStatus;ILjava/util/EnumSet;Lnet/minecraft/world/chunk/ChunkStatus$ChunkType;Lnet/minecraft/world/chunk/ChunkStatus$GenerationTask;Lnet/minecraft/world/chunk/ChunkStatus$LoadTask;)Lnet/minecraft/world/chunk/ChunkStatus;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/registry/Registry;register(Lnet/minecraft/util/registry/Registry;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object replaceRegistry(Registry<? super Object> registry, String id, Object entry) {
        if (id.equals("features")) return LIQUID_CARVERS;
        return Registry.register(registry, id, entry);
    }

    @Shadow @Final @Mutable private static final List<ChunkStatus> DISTANCE_TO_STATUS = ImmutableList.of(
            FULL,
            LIQUID_CARVERS,
            LIQUID_CARVERS,
            BIOMES,
            STRUCTURE_STARTS,
            STRUCTURE_STARTS,
            STRUCTURE_STARTS,
            STRUCTURE_STARTS,
            STRUCTURE_STARTS,
            STRUCTURE_STARTS,
            STRUCTURE_STARTS,
            STRUCTURE_STARTS
    );

}
