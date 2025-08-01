package com.madmike.opapc.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CommandFailureHandler {
    public static int fail(CommandContext<CommandSourceStack> ctx, String message) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            return fail(player, message);
        }
        ctx.getSource().sendFailure(Component.literal(message));
        return 0;
    }

    public static int fail(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
        return 0;
    }
}
