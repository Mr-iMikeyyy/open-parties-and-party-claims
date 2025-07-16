package com.madmike.opapc.war;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.config.OPAPCConfig;
import com.madmike.opapc.features.block.WarBlock;
import com.madmike.opapc.util.NetherClaimAdjuster;
import com.madmike.opapc.war.data.WarData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.*;

public class WarManager {
    public static final WarManager INSTANCE = new WarManager();

    private final List<WarData> activeWars = new ArrayList<>();

    private WarManager() {}

    public List<WarData> getActiveWars() {
        return activeWars;
    }

    public void declareWar(IServerPartyAPI attackerParty, IServerPartyAPI defenderParty, MinecraftServer server) {
        List<BlockPos> warBlocks = spawnWarBlocks(defenderParty, server);
        activeWars.add(new WarData(attackerParty, defenderParty, warBlocks));
        // Drop protections via OPAPC permission API here
        OpenPACServerAPI api = OpenPACServerAPI.get(server);
        api.getPlayerConfigs().getLoadedConfig(defenderParty.getOwner().getUUID()).getUsedSubConfig().tryToSet(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, false);
    }

    public void tick() {
        long currentTime = System.currentTimeMillis();
        activeWars.removeIf(war -> {
            if (currentTime - war.getStartTime() >= OPAPCConfig.warDuration) {
                endWar(war, EndOfWarType.TIMEOUT);
                return true;
            }
            return false;
        });
    }

    public void handleWarBlockBroken(BlockPos pos) {

        for (WarData war : activeWars) {
            if (war.getSpawnedWarBlockPositions().contains(pos)) {
                ChunkPos chunkPos = new ChunkPos(pos);
                OPAPC.getClaimsManager().tryToUnclaim(ServerLevel.OVERWORLD.location(), war.getDefendingParty().getOwner().getUUID(), chunkPos.x, chunkPos.z, chunkPos.x, chunkPos.z, false);

                war.decrementWarBlocksLeft();
                if (war.getWarBlocksLeft() <= 0) {
                    endWar(war, EndOfWarType.ALL_BLOCKS_BROKEN);
                }
                else {
                    war.getAttackingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("War Blocks left to find: " + war.getWarBlocksLeft())));
                    war.getDefendingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("A war block has been destroyed! You have " + war.getWarBlocksLeft() + " left!")));
                }
            }
        }
    }

    public void onPlayerDeath(WarData war, ServerPlayer player, boolean isAttacker) {

        if (isAttacker) {
            if (activeWars.remove(war)){
                war.decrementAttackerLivesRemaining();
                if (war.getAttackerLivesRemaining() <= 0) {
                    endWar(war, EndOfWarType.DEATHS);
                }
                else {
                    war.getAttackingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("Your party has " + attackerLivesRemaining + " lives left!")));
                    war.getDefendingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("Attackers have " + attackerLivesRemaining + " lives left!")));
                }
            }

            activeWars.add(new WarData(war.attackerParty(), war.defenderParty(), war.startTime(), livesLeft));
            activeWars.re
            player.sendSystemMessage(Component.literal("You have " + livesLeft + " lives remaining in this war."));

            BlockPos safePos = findSafePosOutsideClaim(player);
            if (safePos != null) {
                player.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
            }
        } else {
            BlockPos defendPos = findNearestUnclaimBlock(player);
            if (defendPos != null) {
                player.teleportTo(defendPos.getX() + 0.5, defendPos.getY(), defendPos.getZ() + 0.5);
            }
        }
    }

    public enum EndOfWarType {
        TIMEOUT,
        DEATHS,
        FORFEIT,
        ALL_BLOCKS_BROKEN
    }

    public void cleanupWarBlocks(WarData war) {
        for (BlockPos pos : war.getSpawnedWarBlockPositions()) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof WarBlock) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    public void endWar(WarData war, EndOfWarType endType) {

        // Restore protections via OPAPC permission API

        // Award claims stolen or rewards if attackers won

        // Record Stats

        // Teleport enemy players if in claim

        NetherClaimAdjuster.mirrorOverworldClaimsToNether(war.getDefendingParty().getOwner());

        // Optionally notify parties based on end type
        switch (endType) {
            case TIMEOUT -> {
                // handle timeout-specific logic
                cleanupWarBlocks(war, level);
            }
            case DEATHS -> {
                // handle all attacker lives lost logic
                cleanupWarBlocks(war, level);
            }
            case FORFEIT -> {
                // handle forfeiting logic
                cleanupWarBlocks(war, level);
            }
            case ALL_BLOCKS_BROKEN -> {
                // handle block destruction victory logic
            }
        }

        activeWars.remove(war);
    }

    private BlockPos findSafePosOutsideClaim(ServerPlayer player) {
        // TODO: Implement OPAPC claim boundary search to find the nearest safe block outside the claim
        return player.getServerWorld().getSpawnPos();
    }

    private BlockPos findNearestUnclaimBlock(ServerPlayer player) {
        // TODO: Implement OPAPC unclaim block tracking and return a safe position nearby
        ChunkPos chunk = player.getServerWorld().getChunk(player.getBlockPos()).getPos();
        return chunk.getStartPos();
    }
}
