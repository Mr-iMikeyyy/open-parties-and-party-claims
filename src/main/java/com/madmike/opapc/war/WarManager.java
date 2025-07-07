package com.madmike.opapc.war;

import com.madmike.opapc.config.OPAPCConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WarManager {
    public static final WarManager INSTANCE = new WarManager();

    private record WarData(UUID attackerParty, UUID defenderParty, long startTime, int attackerLivesRemaining) {}
    private final Map<UUID, WarData> activeWars = new HashMap<>();

    private WarManager() {}

    public boolean canDeclareWar(UUID attackerPartyId, UUID defenderPartyId, int attackerClaims, int defenderClaims, ServerPlayer player) {
        if (attackerClaims >= defenderClaims && OPAPCConfig.canOnlyAttackLargerClaims) {
            player.sendSystemMessage(Component.literal("You can only declare war on parties with more claims than you."));
            return false;
        }
        if (activeWars.containsKey(defenderPartyId)) {
            player.sendSystemMessage(Component.literal("This party is already under attack."));
            return false;
        }
        return true;
    }

    public void declareWar(UUID attackerPartyId, UUID defenderPartyId) {
        activeWars.put(defenderPartyId, new WarData(attackerPartyId, defenderPartyId, System.currentTimeMillis(), OPAPCConfig.maxAttackerLives));
        // Drop protections via OPAPC permission API here
    }

    public void tick(ServerLevel world) {
        long currentTime = System.currentTimeMillis();
        activeWars.entrySet().removeIf(entry -> {
            WarData data = entry.getValue();
            if (currentTime - data.startTime() >= OPAPCConfig.warDuration || data.attackerLivesRemaining() <= 0) {
                endWar(entry.getKey(), world);
                return true;
            }
            return false;
        });
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

    public void endWar(UUID defenderPartyId, Level level) {
        WarData data = activeWars.remove(defenderPartyId);
        if (data != null) {
            // Restore protections via OPAPC permission API here
            // Award claims stolen or rewards if attackers won
            // Optionally notify parties
        }
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
