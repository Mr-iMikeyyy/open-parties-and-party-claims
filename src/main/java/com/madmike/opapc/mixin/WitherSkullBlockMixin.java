package com.madmike.opapc.mixin;

import net.minecraft.block.WitherSkullBlock;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.pac.common.server.api.OpenPACServerAPI;

import java.util.Objects;

@Mixin(WitherSkullBlock.class)
public class WitherSkullBlockMixin {

    @Inject(method = "Lnet/minecraft/block/WitherSkullBlock;onPlaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/SkullBlockEntity;)V", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/block/WitherSkullBlock;getWitherBossPattern()Lnet/minecraft/block/pattern/BlockPattern;"), cancellable = true)
    private static void onPlacedMixin(World world, BlockPos pos, SkullBlockEntity blockEntity, CallbackInfo ci) {
        if (World.OVERWORLD.equals(Objects.requireNonNull(blockEntity.getWorld()).getRegistryKey()) && OpenPACServerAPI.get(Objects.requireNonNull(world.getServer())).getServerClaimsManager().get(world.getRegistryKey().getRegistry(), pos) == null)
            ci.cancel();
    }
}
