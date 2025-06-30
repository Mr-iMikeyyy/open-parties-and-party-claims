package com.madmike.opapc.command.commands.tele;


import com.madmike.opapc.components.OPAPCComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class HomeCommandHandler {

    @SuppressWarnings("resource")
    public static void handleHomeCommand(ServerPlayer player) {
        BlockPos respawnPos = player.getRespawnPosition();
        ResourceKey<Level> respawnDimension = player.getRespawnDimension();

        if (respawnPos == null) {
            respawnPos = player.serverLevel().getSharedSpawnPos();
            respawnDimension = player.serverLevel().dimension();
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        ServerLevel targetWorld = server.getLevel(respawnDimension);
        if (targetWorld != null) {
            player.teleportTo(
                    targetWorld,
                    respawnPos.getX() + 0.5,
                    respawnPos.getY(),
                    respawnPos.getZ() + 0.5,
                    player.getYRot(),
                    player.getXRot()
            );
        }
        OPAPCComponents.TELE_TIMER.get(player).onTele();
    }
}
