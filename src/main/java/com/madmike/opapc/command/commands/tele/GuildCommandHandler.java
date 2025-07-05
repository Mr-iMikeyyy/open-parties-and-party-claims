package com.madmike.opapc.command.commands.tele;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.data.parties.claims.PartyClaim;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class GuildCommandHandler {

    public static void handleGuildCommand(ServerPlayer player, PartyClaim claim, MinecraftServer server) {
        BlockPos pos = claim.getTeleportPos();

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
            OPAPCComponents.TELE_TIMER.get(player).onTele();
        }
    }
}
