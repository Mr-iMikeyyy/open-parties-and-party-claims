package com.madmike.opapc.war;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCConfig;
import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.features.block.WarBlock;
import com.madmike.opapc.party.data.PartyClaim;
import com.madmike.opapc.util.ClaimAdjacencyChecker;
import com.madmike.opapc.util.NetherClaimAdjuster;
import com.madmike.opapc.util.SafeTeleportHelper;
import com.madmike.opapc.war.data.WarData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
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

    public List<UUID> getPlayersInWar() {
        List<UUID> playersInWar = new ArrayList<>();
        for (WarData war : activeWars) {
            playersInWar.addAll(war.getAttackingPlayers().map(ServerPlayer::getUUID).toList());
            playersInWar.addAll(war.getDefendingPlayers().map(ServerPlayer::getUUID).toList());
        }
        return playersInWar;
    }

    public void declareWar(IServerPartyAPI attackerParty, IServerPartyAPI defenderParty, boolean shouldTeleport) {
        BlockPos warBlock = spawnWarBlock(defenderParty.getOwner().getUUID());


        activeWars.add(new WarData(attackerParty, defenderParty, warBlock));

        // Drop protections via OPAPC permission API here
        OPAPC.getPlayerConfigs().getLoadedConfig(defenderParty.getOwner().getUUID()).getUsedSubConfig().tryToSet(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, false);
    }

    private BlockPos spawnWarBlock(UUID ownerId) {
        List<ChunkPos> ownedChunks = new ArrayList<>();
        OPAPC.getClaimsManager().getPlayerInfo(ownerId)
                .getDimension(Level.OVERWORLD.location())
                .getStream().forEach(e -> e.getStream().forEach(ownedChunks::add));

        RandomSource rand = OPAPC.getServer().overworld().getRandom();
        Level world = OPAPC.getServer().overworld();

        BlockPos result = null;
        int attempts = 0;

        while (result == null && attempts < 2000) {
            attempts++;

            // 1) Pick random owned chunk and random x/z in that chunk
            ChunkPos chunk = ownedChunks.get(rand.nextInt(ownedChunks.size()));
            if (ClaimAdjacencyChecker.wouldBreakAdjacency(chunk, ownedChunks)) {
                continue;
            }
            int baseX = chunk.x << 4;
            int baseZ = chunk.z << 4;
            int x = baseX + rand.nextInt(16);
            int z = baseZ + rand.nextInt(16);

            // 2) Start Y-search from terrain gen height
            BlockPos columnStart = new BlockPos(x, 0, z);
            int startY = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, columnStart).getY();

            for (int y = startY; y < world.getMaxBuildHeight() - 2; y++) {
                BlockPos groundPos = new BlockPos(x, y, z);
                BlockState ground = world.getBlockState(groundPos);

                // Check ground solidity and fluid
                if (!ground.isFaceSturdy(world, groundPos, Direction.UP) || !world.getFluidState(groundPos).isEmpty()) continue;

                // Check required air blocks above
                boolean spaceClear = true;
                for (int i = 1; i <= 2; i++) {
                    if (!world.isEmptyBlock(groundPos.above(i))) {
                        spaceClear = false;
                        break;
                    }
                }
                if (!spaceClear) continue;

                BlockPos spawnPos = groundPos.above(1);

                // Check sky access
                if (!world.canSeeSky(spawnPos)) continue;

                result = spawnPos;
                break; // found a good spot in this column
            }
        }

        return result;
    }

    public void tick() {
        activeWars.removeIf(war -> {
            if (war.isExpired()) {
                endWar(war, EndOfWarType.TIMEOUT);
                return true;
            }
            return false;
        });
    }

    public void onWarBlockBroken(BlockPos pos) {

        for (WarData war : activeWars) {
            if (war.getWarBlockPosition().equals(pos)) {

                ChunkPos chunkPos = new ChunkPos(pos);

                OPAPC.getClaimsManager().tryToUnclaim(ServerLevel.OVERWORLD.location(), war.getDefendingParty().getOwner().getUUID(), chunkPos.x, chunkPos.z, chunkPos.x, chunkPos.z, false);


                war.getDefendingClaim().setBoughtClaims(war.getDefendingClaim().getBoughtClaims() - 1);
                war.getAttackingClaim().setBoughtClaims(war.getAttackingClaim().getBoughtClaims() + 1);

                war.decrementWarBlocksLeft();

                if (war.getWarBlocksLeft() <= 0) {
                    endWar(war, EndOfWarType.ALL_BLOCKS_BROKEN);
                    return;
                }
                else {
                    war.setWarBlockPosition(spawnWarBlock(war.getDefendingParty().getOwner().getUUID()));
                    war.getAttackingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("War Blocks left to find: " + war.getWarBlocksLeft())));
                    war.getDefendingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("A war block has been destroyed! You have " + war.getWarBlocksLeft() + " left!")));
                }

                break;
            }
        }
    }

    public WarData playerIsInWar(UUID playerId) {
        for (WarData war : activeWars) {
            if (war.getAttackingParty().getMemberInfo(playerId) != null || war.getDefendingParty().getMemberInfo(playerId) != null) {
                return war;
            }
        }
        return null;
    }

    public void onPlayerDeath(ServerPlayer player, WarData war) {
        if (war.getAttackingPlayers().anyMatch(e -> e.getUUID().equals(player.getUUID()))) {
            war.decrementAttackerLivesRemaining();
            if (war.getAttackerLivesRemaining() <= 0) {
                endWar(war, EndOfWarType.DEATHS);
            }
            else {
                war.getAttackingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("Your party has " + war.getAttackerLivesRemaining() + " lives left!")));
                war.getDefendingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("Attackers have " + war.getAttackerLivesRemaining() + " lives left!")));
            }
        }
        player.setHealth(player.getMaxHealth());
        SafeTeleportHelper.teleportPlayer(player);
    }


    public enum EndOfWarType {
        TIMEOUT,
        DEATHS,
        FORFEIT,
        ALL_BLOCKS_BROKEN
    }

    public void cleanupWarBlocks(WarData war) {
        if (OPAPC.getServer().overworld().getBlockState(war.getWarBlockPosition()).getBlock() instanceof WarBlock) {
            OPAPC.getServer().overworld().setBlock(war.getWarBlockPosition(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    public void endWar(WarData war, EndOfWarType endType) {

        // Restore protections via OPAPC permission API

        // Award claims stolen or rewards if attackers won

        // Record Stats

        // Teleport enemy players if in claim

        NetherClaimAdjuster.mirrorOverworldClaimsToNether(war.getDefendingParty().getOwner().getUUID());

        // Optionally notify parties based on end type
        switch (endType) {
            case TIMEOUT -> {
                // handle timeout-specific logic
                cleanupWarBlocks(war);
            }
            case DEATHS -> {
                // handle all attacker lives lost logic
                cleanupWarBlocks(war);
            }
            case FORFEIT -> {
                // handle forfeiting logic
                cleanupWarBlocks(war);
            }
            case ALL_BLOCKS_BROKEN -> {
                // handle block destruction victory logic
            }
        }

        activeWars.remove(war);
    }
}
