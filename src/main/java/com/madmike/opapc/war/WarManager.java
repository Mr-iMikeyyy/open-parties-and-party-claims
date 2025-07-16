package com.madmike.opapc.war;

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
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
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

//    public boolean canDeclareWar(UUID attackerPartyId, UUID defenderPartyId, int attackerClaims, int defenderClaims, ServerPlayer player) {
//        if (attackerClaims >= defenderClaims && OPAPCConfig.canOnlyAttackLargerClaims) {
//            player.sendSystemMessage(Component.literal("You can only declare war on parties with more claims than you."));
//            return false;
//        }
//        for (WarData war : activeWars) {
//            if (war.getAttackingParty().getId().equals(attackerPartyId)) {
//                player.sendSystemMessage(Component.literal("You are already in a war!"));
//                return false;
//            }
//            if (war.getDefendingParty().getId().equals(defenderPartyId)) {
//                player.sendSystemMessage(Component.literal("This party is already under attack."));
//                return false;
//            }
//        }
//        return true;
//    }

    public void declareWar(IServerPartyAPI attackerParty, IServerPartyAPI defenderParty, MinecraftServer server) {
        List<BlockPos> warBlocks = spawnWarBlocks(defenderParty, server);
        activeWars.add(new WarData(attackerParty, defenderParty, warBlocks));
        // Drop protections via OPAPC permission API here
        OpenPACServerAPI api = OpenPACServerAPI.get(server);
        api.getPlayerConfigs().getLoadedConfig(defenderParty.getOwner().getUUID()).getUsedSubConfig().tryToSet(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, false);
    }

    public void tick(ServerLevel level) {
        long currentTime = System.currentTimeMillis();
        activeWars.removeIf(war -> {
            if (currentTime - war.getStartTime() >= OPAPCConfig.warDuration) {
                endWar(war, EndOfWarType.TIMEOUT);
                return true;
            }
            return false;
        });
    }

    public void handleWarBlockBroken(ServerLevel level, BlockPos pos) {

        for (WarData war : activeWars) {
            if (war.getSpawnedWarBlockPositions().contains(pos)) {
                OpenPACServerAPI api = OpenPACServerAPI.get(level.getServer());
                IServerClaimsManagerAPI cm = api.getServerClaimsManager();
                ChunkPos chunkPos = new ChunkPos(pos);
                cm.tryToUnclaim(ServerLevel.OVERWORLD.location(), war.getDefendingParty().getOwner().getUUID(), chunkPos.x, chunkPos.z, chunkPos.x, chunkPos.z, false);

                war.decrementWarBlocksLeft(level);

                war.getAttackingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("War Blocks left to find: " + war.ge)));
            }
        }

        ChunkPos chunkPos = level.getChunk(pos).getPos();

        IClaimManagerAPI claimManager = OpenPACServerAPI.get(world.getServer()).getClaimManager();
        if (claimManager.isChunkClaimed(chunkPos)) {
            unclaimChunk(chunkPos);

            level.ge.getPlayers().forEach(player ->
                    player.s(Component.literal("§aChunk at " + chunkPos + " has been unclaimed!"), false));
        }
    }

    public void onPlayerDeath(ServerPlayer player, UUID playerPartyId, boolean isAttacker) {
        WarData war = activeWars.values().stream()
                .filter(w -> w.attackerParty().equals(playerPartyId) || w.defenderParty().equals(playerPartyId))
                .findFirst().orElse(null);

        if (war == null) return;

        if (isAttacker) {
            int livesLeft = war.attackerLivesRemaining() - 1;
            activeWars.put(war.defenderParty(), new WarData(war.attackerParty(), war.defenderParty(), war.startTime(), livesLeft));
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

    public void cleanupWarBlocks(WarData war, ServerLevel level) {
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

        NetherClaimAdjuster.mirrorOverworldClaimsToNether(api.getServerClaimsManager(), war.getDefendingParty().getOwner());

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
