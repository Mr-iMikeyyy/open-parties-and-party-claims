package com.madmike.opapc.war;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.data.PartyClaim;
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
                BlockPos warpPos = findOutsideRespawn(war.getDefendingClaim());
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
            if (ClaimAdjacencyChecker.wouldBreakAdjacency(war.getDefendingParty().getOwner().getUUID(), chunk)) {
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

                OPAPC.getClaimsManager().unclaim(ServerLevel.OVERWORLD.location(), chunkPos.x, chunkPos.z);

                war.getDefendingClaim().setBoughtClaims(war.getDefendingClaim().getBoughtClaims() - 1);
                war.getAttackingClaim().setBoughtClaims(war.getAttackingClaim().getBoughtClaims() + 1);

                war.decrementWarBlocksLeft();

                if (war.getWarBlocksLeft() <= 0) {
                    endWar(war, EndOfWarType.ALL_BLOCKS_BROKEN);
                    return;
                }
                else {
                    war.setWarBlockPosition(spawnWarBlock(war));
                    war.getAttackingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("War Blocks left to find: " + war.getWarBlocksLeft())));
                    war.getDefendingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("A war block has been destroyed! You have " + war.getWarBlocksLeft() + " left!")));
                }

                break;
            }
        }
    }

    public void onDefenderDeath(ServerPlayer player, WarData war) {
        player.setHealth(player.getMaxHealth());
        PartyClaim claim = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard())
                .getClaim(war.getDefendingParty().getId());
        Random rand = new Random();
        ServerLevel level = OPAPC.getServer().overworld();

        // Warp to warp point if it's the last claim
        if (claim.getClaimedChunksList().size() == 1) {
            BlockPos warpPos = claim.getWarpPos();
            player.teleportTo(level, warpPos.getX() + 0.5, warpPos.getY(), warpPos.getZ() + 0.5, player.getYRot(), player.getXRot());
            return;
        }

        // Pick a random claimed chunk
        List<ChunkPos> claimedChunks = new ArrayList<>(claim.getClaimedChunksList());
        ChunkPos chosenChunk = claimedChunks.get(rand.nextInt(claimedChunks.size()));

        // Convert chunk to world coordinates
        int baseX = chosenChunk.x << 4;
        int baseZ = chosenChunk.z << 4;

        BlockPos safePos = null;

        // Try up to 50 times to find a safe spot
        for (int attempt = 0; attempt < 50 && safePos == null; attempt++) {
            int x = baseX + rand.nextInt(16);
            int z = baseZ + rand.nextInt(16);

            // Start at terrain surface height
            int startY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            safePos = findSafeY(x, startY, z);
        }

        // Fallback to chunk center if none found
        if (safePos == null) {
            int centerX = baseX + 8;
            int centerZ = baseZ + 8;
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ);
            safePos = new BlockPos(centerX, surfaceY, centerZ);
        }
        else {
            endWar(war, EndOfWarType.BUG);
        }

        // Teleport player
        player.teleportTo(level, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    public void onAttackerDeath(ServerPlayer player, WarData war) {
        war.decrementAttackerLivesRemaining();
        if (war.getWarBlocksLeft() <= 0) {
            endWar(war, EndOfWarType.ALL_BLOCKS_BROKEN);
        }
        player.setHealth(player.getMaxHealth());

        PartyClaim claim = OPAPCComponents.PARTY_CLAIMS
                .get(OPAPC.getServer().getScoreboard())
                .getClaim(war.getDefendingParty().getId());
        ServerLevel level = OPAPC.getServer().overworld();

        if (war.getAttackerLivesRemaining() <= 0) {
            endWar(war, EndOfWarType.DEATHS);
            return;
        }

        // Try to find a respawn point outside of claim
        BlockPos respawnPos = findOutsideRespawn(claim);

        if (respawnPos != null) {
            player.teleportTo(level, respawnPos.getX() + 0.5, respawnPos.getY(), respawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
        } else {
            // Fallback: spawn at world spawn if no safe spot found
            endWar(war, EndOfWarType.BUG);
        }

        // Notify players
        war.getAttackingPlayers().forEach(p ->
                p.sendSystemMessage(Component.literal("Your party has " + war.getAttackerLivesRemaining() + " lives left!")));
        war.getDefendingPlayers().forEach(p ->
                p.sendSystemMessage(Component.literal("Attackers have " + war.getAttackerLivesRemaining() + " lives left!")));
    }

    private BlockPos findOutsideRespawn(PartyClaim claim) {
        Set<ChunkPos> claimed = new HashSet<>(claim.getClaimedChunksList());
        Random rand = new Random();

        // Pick a random claimed chunk as starting point
        List<ChunkPos> claimedList = new ArrayList<>(claimed);
        if (claimedList.isEmpty()) return null;
        ChunkPos startChunk = claimedList.get(rand.nextInt(claimedList.size()));

        // Directions to search outward
        ChunkPos[] directions = {
                new ChunkPos(1, 0),   // east
                new ChunkPos(-1, 0),  // west
                new ChunkPos(0, 1),   // south
                new ChunkPos(0, -1)   // north
        };

        // Shuffle search directions
        List<ChunkPos> dirList = Arrays.asList(directions);
        Collections.shuffle(dirList);

        for (ChunkPos dir : dirList) {
            ChunkPos check = startChunk;
            for (int distance = 2; distance <= 8; distance++) { // search up to 8 chunks away
                check = new ChunkPos(check.x + dir.x, check.z + dir.z);

                if (!claimed.contains(check)) {
                    // Found an unclaimed chunk
                    int baseX = check.x << 4;
                    int baseZ = check.z << 4;

                    // Try 20 random positions inside the chunk
                    for (int i = 0; i < 20; i++) {
                        int x = baseX + rand.nextInt(16);
                        int z = baseZ + rand.nextInt(16);
                        int startY = OPAPC.getServer().overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                        BlockPos pos = findSafeY( x, startY, z);
                        if (pos != null) {
                            return pos;
                        }
                    }
                }
            }
        }
        return null; // nothing found
    }

    private BlockPos findSafeY(int x, int startY, int z) {
        // Check downward from startY
        ServerLevel level = OPAPC.getServer().overworld();
        for (int y = startY; y > level.getMaxBuildHeight(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState below = level.getBlockState(pos.below());
            BlockState block = level.getBlockState(pos);
            BlockState above = level.getBlockState(pos.above());

            if (below.isSolid() && block.isAir() && above.isAir() && level.canSeeSky(pos)) {
                return pos;
            }
        }
        return null;
    }






    public enum EndOfWarType {
        TIMEOUT,
        DEATHS,
        BUG,
        ALL_BLOCKS_BROKEN
    }

    public void endWar(WarData war, EndOfWarType endType) {

        // Restore protections via OPAPC permission API
        OPAPC.getPlayerConfigs().getLoadedConfig(war.getDefendingParty().getOwner().getUUID()).getUsedSubConfig().tryToSet(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, true);

        // Award claims stolen or rewards if attackers won

        // Record Stats

        // Teleport enemy players if in claim

        NetherClaimAdjuster.mirrorOverworldClaimsToNether(war.getDefendingParty().getOwner().getUUID());

        // Optionally notify parties based on end type
        switch (endType) {
            case TIMEOUT -> {
                // handle timeout-specific logic
                cleanupWarBlock(war);
            }
            case DEATHS -> {
                // handle all attacker lives lost logic
                cleanupWarBlock(war);
            }
            case BUG -> {
                // handle forfeiting logic
                cleanupWarBlock(war);
            }
            case ALL_BLOCKS_BROKEN -> {
                // handle block destruction victory logic
            }
        }

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
