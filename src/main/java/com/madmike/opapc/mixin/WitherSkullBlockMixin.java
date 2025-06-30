package com.madmike.opapc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherSkullBlock.class)
public class WitherSkullBlockMixin {

    @Inject(
            method = "checkSpawn",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void denyWitherInOverworld(Level level, BlockPos pos, SkullBlockEntity skullBlockEntity, CallbackInfo ci) {
        if (!level.isClientSide() && Level.OVERWORLD.equals(level.dimension())) {
            ci.cancel(); // Prevent Wither spawning in Overworld
        }
    }
}
