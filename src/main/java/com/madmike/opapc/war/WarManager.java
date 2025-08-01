package com.madmike.opapc.war;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.util.SafeWarpFinder;
import com.madmike.opapc.war.features.block.WarBlock;
import com.madmike.opapc.util.ClaimAdjacencyChecker;
import com.madmike.opapc.util.NetherClaimAdjuster;
import com.madmike.opapc.war.data.WarData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
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

    public void declareWar(IServerPartyAPI attackerParty, IServerPartyAPI defenderParty, boolean shouldWarp) {
        WarData war = new WarData(attackerParty, defenderParty);
        war.setWarBlockPosition(spawnWarBlock(war));
        if (shouldWarp) {
            for (ServerPlayer player : war.getAttackingPlayers()) {
                BlockPos warpPos = SafeWarpFinder.findSafeSpawnOutsideClaim(war.getDefendingClaim());
                if (warpPos != null) {
                    player.teleportTo(OPAPC.getServer().overworld(), warpPos.getX() + 0.5, warpPos.getY(), warpPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                }
                else {
                    endWar(war, EndOfWarType.BUG);
                    break;
                }
            }
            war.setWarp(true);
        }
        else {
            war.setWarp(false);
        }
        activeWars.add(war);

        for (ServerPlayer player : war.getDefendingPlayers()) {
            player.sendSystemMessage(Component.literal("Your claim is under attack!"));

            Component clickableMessage = Component.literal("§6[Click here to warp to your party claim]")
                    .withStyle(style -> style
                            .withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    "/warp party" // <-- your warp command
                            ))
                            .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Warp back to your party's warp point")
                            ))
                    );
            player.sendSystemMessage(clickableMessage);
            displayWarInfo(player);
        }

        for (ServerPlayer player : war.getAttackingPlayers()) {
            displayWarInfo(player);
        }

        // Drop protections via OPAPC permission API here
        OPAPC.getPlayerConfigs().getLoadedConfig(defenderParty.getOwner().getUUID()).getUsedSubConfig().tryToSet(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, false);
    }

    private BlockPos spawnWarBlock(WarData war) {
        PartyClaim claim = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getClaim(war.getDefendingParty().getId());
        List<ChunkPos> ownedChunks = claim.getClaimedChunksList();


        if (ownedChunks.size() > 1) {
            ownedChunks.remove(new ChunkPos(claim.getWarpPos()));
        }

        RandomSource rand = OPAPC.getServer().overworld().getRandom();
        Level world = OPAPC.getServer().overworld();

        BlockPos result = null;
        int attempts = 0;

        while (result == null && attempts < 2000) {
            attempts++;

            // 1) Pick random owned chunk and random x/z in that chunk
            ChunkPos chunk = ownedChunks.get(rand.nextInt(ownedChunks.size()));
            if (ClaimAdjacencyChecker.wouldBreakAdjacency(claim.getClaimedChunksList(), chunk)) {
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
                endWar(war, EndOfWarType.ATTACKERS_LOSE);
                return true;
            }
            return false;
        });
    }

    public void onWarBlockBroken(BlockPos pos) {
        // Early exit if no wars are active
        if (activeWars.isEmpty()) return;

        ChunkPos brokenChunk = new ChunkPos(pos);

        for (Iterator<WarData> it = activeWars.iterator(); it.hasNext();) {
            WarData war = it.next();

            if (!pos.equals(war.getWarBlockPosition())) continue;

            // Unclaim the chunk
            OPAPC.getClaimsManager().unclaim(ServerLevel.OVERWORLD.location(), brokenChunk.x, brokenChunk.z);

            // Update defending claim
            PartyClaim defendingClaim = war.getDefendingClaim();
            defendingClaim.setBoughtClaims(defendingClaim.getBoughtClaims() - 1);
            defendingClaim.incrementClaimsLostToWar();

            // Update attacking claim
            PartyClaim attackingClaim = war.getAttackingClaim();
            attackingClaim.setBoughtClaims(attackingClaim.getBoughtClaims() + 1);
            attackingClaim.incrementClaimsGainedFromWar();

            if (defendingClaim.getBoughtClaims() <= 0) {
                endWar(war, EndOfWarType.ANNIHILATED);
                return; // done, no need to check further
            }

            // Decrement remaining war blocks
            war.decrementWarBlocksLeft();
            int warBlocksLeft = war.getWarBlocksLeft();

            if (warBlocksLeft <= 0) {
                endWar(war, EndOfWarType.ATTACKERS_WIN);
                return; // done, no need to check further
            }

            // Spawn new war block and notify players
            BlockPos newBlock = spawnWarBlock(war);
            if (newBlock != null) {
                war.setWarBlockPosition(newBlock);
            }
            else {
                endWar(war, EndOfWarType.BUG);
                return;
            }

            Component attackerMsg = Component.literal("War Blocks left to find: " + warBlocksLeft);
            Component defenderMsg = Component.literal("A war block has been destroyed! You have " + warBlocksLeft + " left!");

            war.getAttackingPlayers().forEach(p -> p.sendSystemMessage(attackerMsg));
            war.getDefendingPlayers().forEach(p -> p.sendSystemMessage(defenderMsg));

            return; // stop after handling the first relevant war
        }
    }

    public void onDefenderDeath(ServerPlayer player, WarData war) {
        player.setHealth(player.getMaxHealth());
        PartyClaim claim = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard())
                .getClaim(war.getDefendingParty().getId());

        ServerLevel level = OPAPC.getServer().overworld();

        // Warp to warp point if it's the last claim
        if (claim.getClaimedChunksList().size() == 1) {
            BlockPos warpPos = claim.getWarpPos();
            player.teleportTo(level, warpPos.getX() + 0.5, warpPos.getY(), warpPos.getZ() + 0.5, player.getYRot(), player.getXRot());
            return;
        }

        BlockPos safePos = SafeWarpFinder.findSafeSpawnInsideClaim(claim);

        if (safePos == null) {
            safePos = claim.getWarpPos();
        }

        if (safePos != null) {
            player.teleportTo(OPAPC.getServer().overworld(), safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, player.getYRot(), player.getXRot());
        }
        else {
            endWar(war, EndOfWarType.BUG);
        }
    }

    public void onAttackerDeath(ServerPlayer player, WarData war) {
        player.setHealth(player.getMaxHealth());

        war.decrementAttackerLivesRemaining();
        if (war.getAttackerLivesRemaining() <= 0) {
            endWar(war, EndOfWarType.ATTACKERS_LOSE);
        }

        PartyClaim claim = OPAPCComponents.PARTY_CLAIMS
                .get(OPAPC.getServer().getScoreboard())
                .getClaim(war.getDefendingParty().getId());
        ServerLevel level = OPAPC.getServer().overworld();

        BlockPos respawnPos = SafeWarpFinder.findSafeSpawnOutsideClaim(claim);

        if (respawnPos != null) {
            player.teleportTo(level, respawnPos.getX() + 0.5, respawnPos.getY(), respawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
        } else {
            endWar(war, EndOfWarType.BUG);
        }

        // Notify players
        war.getAttackingPlayers().forEach(p ->
                p.sendSystemMessage(Component.literal("Your party has " + war.getAttackerLivesRemaining() + " lives left!")));
        war.getDefendingPlayers().forEach(p ->
                p.sendSystemMessage(Component.literal("Attackers have " + war.getAttackerLivesRemaining() + " lives left!")));
    }

    public enum EndOfWarType {
        ATTACKERS_WIN,
        ATTACKERS_LOSE,
        ANNIHILATED,
        BUG,
    }

    public void endWar(WarData war, EndOfWarType endType) {
        cleanupWarBlock(war);

        // Restore protections via OPAPC permission API
        OPAPC.getPlayerConfigs().getLoadedConfig(war.getDefendingParty().getOwner().getUUID())
                .getUsedSubConfig()
                .tryToSet(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, true);

        // Handle attacker teleports
        if (war.getWarp()) {
            for (ServerPlayer player : war.getAttackingPlayers()) {
                BlockPos warpPos = war.getAttackingClaim().getWarpPos();
                player.teleportTo(OPAPC.getServer().overworld(), warpPos.getX() + 0.5, warpPos.getY(),
                        warpPos.getZ() + 0.5, player.getYRot(), player.getXRot());
            }
        } else {
            for (ServerPlayer player : war.getAttackingPlayers()) {
                if (war.getDefendingClaim().getClaimedChunksList().contains(player.chunkPosition())) {
                    BlockPos safeSpawn = SafeWarpFinder.findSafeSpawnOutsideClaim(war.getDefendingClaim());
                    if (safeSpawn != null) {
                        player.teleportTo(OPAPC.getServer().overworld(), safeSpawn.getX() + 0.5, safeSpawn.getY(),
                                safeSpawn.getZ() + 0.5, player.getYRot(), player.getXRot());
                    } else {
                        BlockPos warpPos = war.getAttackingClaim().getWarpPos();
                        player.teleportTo(OPAPC.getServer().overworld(), warpPos.getX() + 0.5, warpPos.getY(),
                                warpPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                    }
                }
            }
        }

        NetherClaimAdjuster.mirrorOverworldClaimsToNether(war.getDefendingParty().getOwner().getUUID());

        // Get party names
        String attackers = war.getAttackingClaim().getPartyName();
        String defenders = war.getDefendingClaim().getPartyName();

        // Build announcement
        String message;
        switch (endType) {
            case ATTACKERS_LOSE -> {
                war.getAttackingClaim().incrementWarAttacksLost();
                war.getDefendingClaim().incrementWarDefencesWon();
                message = "§c" + defenders + " have successfully defended their land against " + attackers + "!";
            }
            case ATTACKERS_WIN -> {
                war.getAttackingClaim().incrementWarAttacksWon();
                war.getDefendingClaim().incrementWarDefencesLost();
                message = "§a" + attackers + " were victorious! " + defenders + " have lost the war.";
            }
            case ANNIHILATED -> {
                message = attackers + " have annihilated " + defenders + "... They have been wiped off the map.";
                OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).removeClaim(war.getDefendingClaim().getPartyId());
            }
            case BUG -> {
                message = "§eThe war between " + attackers + " and " + defenders + " has ended abruptly due to an error, contact admin.";
            }
            default -> {
                message = "§7The war between " + attackers + " and " + defenders + " has ended.";
            }
        }

        // Broadcast server-wide
        OPAPC.getServer().getPlayerList()
                .broadcastSystemMessage(Component.literal(message), false);

        activeWars.remove(war);
    }

    public void cleanupWarBlock(WarData war) {
        if (OPAPC.getServer().overworld().getBlockState(war.getWarBlockPosition()).getBlock() instanceof WarBlock) {
            OPAPC.getServer().overworld().setBlock(war.getWarBlockPosition(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    public void displayWarInfo(ServerPlayer player) {

        for (WarData war : activeWars) {
            boolean isAttacker = war.getAttackingPlayers().contains(player);
            boolean isDefender = war.getDefendingPlayers().contains(player);

            if (isAttacker || isDefender ) {
                Component header = Component.literal("§6--- War Info ---");
                Component role = Component.literal("§eRole: " + (isAttacker ? "Attacker" : "Defender"));
                Component attackingParty = Component.literal("§cAttacking Party: " + war.getAttackingPartyName());
                Component defendingParty = Component.literal("§aDefending Party: " + war.getDefendingPartyName());

                long timeLeftMillis = (war.getStartTime() + (war.getDurationSeconds() * 1000L)) - System.currentTimeMillis();
                long minutes = Math.max(0, timeLeftMillis / 60000);
                long seconds = Math.max(0, (timeLeftMillis % 60000) / 1000);
                Component timeLeft = Component.literal("§bTime Remaining: " + minutes + "m " + seconds + "s");

                Component warBlocks = Component.literal("§dWar Blocks: " + war.getWarBlocksLeft());
                Component attackerLives = Component.literal("§4Attacker Lives Left: " + war.getAttackerLivesRemaining());

                player.sendSystemMessage(header);
                player.sendSystemMessage(role);
                player.sendSystemMessage(attackingParty);
                player.sendSystemMessage(defendingParty);
                player.sendSystemMessage(timeLeft);
                player.sendSystemMessage(warBlocks);
                player.sendSystemMessage(attackerLives);
                return;
            }
        }

        player.sendSystemMessage(Component.literal("§7You are not currently in an active war."));
    }
}
