package com.madmike.opapc.mixin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.pac.common.server.claims.command.ClaimsClaimCommands;
import xaero.pac.common.server.player.data.ServerPlayerData;
import xaero.pac.common.server.player.data.api.ServerPlayerDataAPI;

@Mixin(ClaimsClaimCommands.class)
public abstract class ClaimsClaimCommandsMixin {

    @Redirect(
            method = "createClaimCommand",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/builder/ArgumentBuilder;executes(Lcom/mojang/brigadier/Command;)Lcom/mojang/brigadier/builder/ArgumentBuilder;"
            ),
            remap = false
    )
    private static ArgumentBuilder<CommandSourceStack, ?> redirectClaimExecutes(
            ArgumentBuilder<CommandSourceStack, ?> builder,
            Command<CommandSourceStack> originalCommand
    ) {
        return builder.executes(context -> {
            ServerPlayer player = context.getSource().getPlayer();

            ServerPlayerData playerData = player != null ? (ServerPlayerData) ServerPlayerDataAPI.from(player) : null;

            if (playerData != null) {
                if (playerData.isClaimsServerMode() || playerData.isClaimsAdminMode()) {
                    return originalCommand.run(context);
                }
            }

            if (player != null) {
                context.getSource().sendFailure(Component.literal(
                        "This claim command is disabled. Please use /partyclaim instead."
                ));
            }

            return 0;
        });
    }
}
