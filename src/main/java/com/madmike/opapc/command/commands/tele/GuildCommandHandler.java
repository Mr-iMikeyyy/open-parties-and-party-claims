package com.madmike.opapc.command.commands.tele;

import com.madmike.opapc.data.parties.claims.PartyClaim;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GuildCommandHandler {
    public static void handleGuildCommand(ServerPlayerEntity player, PartyClaim claim, MinecraftServer server) {
        BlockPos pos = claim.getPcbBlockPos();

        player.teleport(server.getWorld(World.OVERWORLD), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), player.getPitch());
    }
}
