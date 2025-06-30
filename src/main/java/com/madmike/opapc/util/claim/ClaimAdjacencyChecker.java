package com.madmike.opapc.util.claim;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaero.pac.common.claims.player.api.IPlayerClaimPosListAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.player.api.IServerPlayerClaimInfoAPI;
import xaero.pac.common.server.parties.party.IServerParty;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.*;

public class ClaimAdjacencyChecker {

    public static boolean wouldBreakAdjacency(
            IServerPartyAPI party,
            ResourceKey<Level> dimension,
            ChunkPos toUnclaim,
            OpenPACServerAPI api
    ) {
        IServerPlayerClaimInfoAPI leaderInfo = api.getServerClaimsManager().getPlayerInfoStream()
                .filter(e -> e.getPlayerId().equals(party.getOwner().getUUID()))
                .findFirst()
                .orElse(null);

        if (leaderInfo == null) return false;

        List<IPlayerClaimPosListAPI> allLists = leaderInfo.getDimension(dimension.location()).getStream().toList();
        Set<ChunkPos> allClaims = new HashSet<>();

        for (IPlayerClaimPosListAPI list : allLists) {
            list.getStream().forEach(allClaims::add);
        }

        allClaims.remove(toUnclaim);

        if (allClaims.isEmpty()) {
            return true; // If no claims remain, they are trivially connected
        }

        Set<ChunkPos> visited = new HashSet<>();
        Queue<ChunkPos> toVisit = new ArrayDeque<>();
        ChunkPos start = allClaims.iterator().next();
        toVisit.add(start);

        while (!toVisit.isEmpty()) {
            ChunkPos current = toVisit.poll();
            if (!visited.add(current)) continue;

            for (ChunkPos neighbor : getNeighbors(current)) {
                if (allClaims.contains(neighbor) && !visited.contains(neighbor)) {
                    toVisit.add(neighbor);
                }
            }
        }

        return visited.size() != allClaims.size(); // True if disconnected after removal
    }

    public static boolean isAdjacentToExistingClaim(
            IServerPartyAPI party,
            ResourceKey<Level> dimension,
            ChunkPos targetChunk,
            OpenPACServerAPI api
    ) {
        IServerPlayerClaimInfoAPI leaderInfo = api.getServerClaimsManager().getPlayerInfoStream()
                .filter(e -> e.getPlayerId().equals(party.getOwner().getUUID()))
                .findFirst()
                .orElse(null);

        if (leaderInfo == null) return false;

        List<IPlayerClaimPosListAPI> allLists = leaderInfo.getDimension(dimension.location()).getStream().toList();
        Set<ChunkPos> allClaims = new HashSet<>();

        for (IPlayerClaimPosListAPI list : allLists) {
            list.getStream().forEach(allClaims::add);
        }

        if (allClaims.isEmpty()) {
            return true; // First claim always allowed
        }

        for (ChunkPos neighbor : getNeighbors(targetChunk)) {
            if (allClaims.contains(neighbor)) {
                return true;
            }
        }

        return false;
    }

    private static List<ChunkPos> getNeighbors(ChunkPos pos) {
        return List.of(
                new ChunkPos(pos.x + 1, pos.z),
                new ChunkPos(pos.x - 1, pos.z),
                new ChunkPos(pos.x, pos.z + 1),
                new ChunkPos(pos.x, pos.z - 1)
        );
    }
}
