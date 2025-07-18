package com.madmike.opapc.war;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.config.OPAPCConfig;
import com.madmike.opapc.features.block.WarBlock;
import com.madmike.opapc.util.NetherClaimAdjuster;
import com.madmike.opapc.util.SafeTeleportHelper;
import com.madmike.opapc.war.data.WarData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
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

    public List<UUID> getPlayersInWar() {
        List<UUID> playersInWar = new ArrayList<>();
        for (WarData war : activeWars) {
            playersInWar.addAll(war.getAttackingPlayers().map(ServerPlayer::getUUID).toList());
            playersInWar.addAll(war.getDefendingPlayers().map(ServerPlayer::getUUID).toList());
        }
        return playersInWar;
    }

    public void declareWar(IServerPartyAPI attackerParty, IServerPartyAPI defenderParty) {
        List<BlockPos> warBlocks = spawnWarBlocks(defenderParty.getOwner().getUUID());
        activeWars.add(new WarData(attackerParty, defenderParty));

        // Drop protections via OPAPC permission API here
        OPAPC.getPlayerConfigs().getLoadedConfig(defenderParty.getOwner().getUUID()).getUsedSubConfig().tryToSet(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, false);
    }

    private List<BlockPos> spawnWarBlocks(UUID ownerId) {

        List<ChunkPos> ownedChunks = new ArrayList<>();
        OPAPC.getClaimsManager().getPlayerInfo(ownerId).getDimension(Level.OVERWORLD.location()).getStream().forEach(e -> e.getStream().forEach(ownedChunks::add));

        RandomSource rand = OPAPC.getServer().overworld().getRandom();

        List<BlockPos> results = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        int attempts = 0;

        while (results.size() < OPAPCConfig.maxClaimBlocksPerWar && attempts < 2000) {
            attempts++;
            // 1) choose a random chunk
            ChunkPos chunk = ownedChunks.get(rand.nextInt(ownedChunks.size()));
            int baseX = chunk.x << 4, baseZ = chunk.z << 4;

            // 2) choose a random (x,z) inside it
            int x = baseX + rand.nextInt(16);
            int z = baseZ + rand.nextInt(16);

            BlockPos pos = new BlockPos(x, 0 , z);

            // 3) find the surface
            Level world = OPAPC.getServer().overworld();
            BlockPos surface = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos);
            BlockPos spawnPos = surface.above();

            // 4) check sky
            if (!world.canSeeSky(spawnPos)) continue;

            // 5) check ground solidity
            BlockState ground = world.getBlockState(surface);
            if (!ground.isFaceSturdy(world, surface, Direction.UP) || !world.getFluidState(surface).isEmpty()) continue;

            // 6) check headspace
            boolean clear = true;
            for (int i = 0; i < OPAPCConfig.airRequiredAboveWarBlocks; i++) {
                if (!world.isEmptyBlock(spawnPos.above(i))) {
                    clear = false;
                    break;
                }
            }
            if (!clear) continue;

            // 7) dedupe
            if (seen.add(spawnPos)) {
                results.add(spawnPos);
            }

        }

        return results;
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

    public void onWarBlockBroken(BlockPos pos) {

        for (WarData war : activeWars) {
            if (war.getSpawnedWarBlockPositions().contains(pos)) {

                ChunkPos chunkPos = new ChunkPos(pos);

                OPAPC.getClaimsManager().tryToUnclaim(ServerLevel.OVERWORLD.location(), war.getDefendingParty().getOwner().getUUID(), chunkPos.x, chunkPos.z, chunkPos.x, chunkPos.z, false);

                war.decrementWarBlocksLeft();

                if (war.getWarBlocksLeft() <= 0) {
                    endWar(war, EndOfWarType.ALL_BLOCKS_BROKEN);
                    return;
                }
                else {
                    war.getAttackingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("War Blocks left to find: " + war.getWarBlocksLeft())));
                    war.getDefendingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("A war block has been destroyed! You have " + war.getWarBlocksLeft() + " left!")));
                }
            }
        }
    }

    public void onPlayerDeath(ServerPlayer player) {
        for (WarData war : activeWars) {
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
            healPlayer(player);
            SafeTeleportHelper.teleportPlayer(player);
        }
    }

    private void healPlayer(ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
    }

    public enum EndOfWarType {
        TIMEOUT,
        DEATHS,
        FORFEIT,
        ALL_BLOCKS_BROKEN
    }

    public void cleanupWarBlocks(WarData war) {
        for (BlockPos pos : war.getSpawnedWarBlockPositions()) {
            if (OPAPC.getServer().overworld().getBlockState(pos).getBlock() instanceof WarBlock) {
                OPAPC.getServer().overworld().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
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
