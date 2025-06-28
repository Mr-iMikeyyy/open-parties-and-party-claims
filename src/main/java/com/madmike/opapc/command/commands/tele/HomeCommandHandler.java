package com.madmike.opapc.command.commands.tele;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HomeCommandHandler {
    public static void handleHomeCommand(ServerPlayerEntity player) {
        BlockPos respawnPos = player.getSpawnPointPosition();
        RegistryKey<World> respawnDimension = player.getSpawnPointDimension();

        if (respawnPos == null) {
            respawnPos = player.getServerWorld().getSpawnPos();
            respawnDimension = player.getServerWorld().getRegistryKey();
        }

        ServerWorld targetWorld = player.getServer().getWorld(respawnDimension);
        if (targetWorld != null && respawnPos != null) {
            player.teleport(targetWorld, respawnPos.getX() + 0.5, respawnPos.getY(), respawnPos.getZ() + 0.5, player.getYaw(), player.getPitch());
        }
    }
}
