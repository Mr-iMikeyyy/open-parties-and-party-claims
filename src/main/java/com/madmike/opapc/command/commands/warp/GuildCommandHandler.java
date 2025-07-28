package com.madmike.opapc.command.commands.warp;

import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class GuildCommandHandler {

    public static void handleGuildCommand(ServerPlayer player, PartyClaim claim, MinecraftServer server) {
        BlockPos pos = claim.getWarpPos();

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);

        if (overworld != null && pos != null) {
            player.teleportTo(
                    overworld,
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5,
                    player.getYRot(), // yaw
                    player.getXRot()  // pitch
            );
            OPAPCComponents.WARP_TIMER.get(player).onWarp();
        }
    }
}
