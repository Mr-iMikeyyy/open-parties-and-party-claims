package com.madmike.opapc.claim;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import xaero.pac.common.claims.player.IPlayerChunkClaim;
import xaero.pac.common.claims.player.IPlayerClaimPosList;
import xaero.pac.common.claims.player.IPlayerDimensionClaims;
import xaero.pac.common.claims.player.api.IPlayerClaimPosListAPI;
import xaero.pac.common.server.claims.IServerClaimsManager;
import xaero.pac.common.server.claims.IServerDimensionClaimsManager;
import xaero.pac.common.server.claims.IServerRegionClaims;
import xaero.pac.common.server.claims.player.IServerPlayerClaimInfo;
import xaero.pac.common.server.claims.player.api.IServerPlayerClaimInfoAPI;
import xaero.pac.common.server.parties.party.IServerParty;

import java.util.*;

public class ClaimAdjacencyChecker {

    public static boolean wouldBreakAdjacency(
            IServerParty<?, ?, ?> party,
            RegistryKey<World> dimension,
            ChunkPos toUnclaim,
            IServerClaimsManager<IPlayerChunkClaim, IServerPlayerClaimInfo<IPlayerDimensionClaims<IPlayerClaimPosList>>, IServerDimensionClaimsManager<IServerRegionClaims>> claimsManager
    ) {
        // Get the party leader’s claim info
        IServerPlayerClaimInfoAPI leaderInfo = claimsManager.getPlayerInfoStream()
                .filter(e -> e.getPlayerId().equals(party.getOwner().getUUID()))
                .findFirst()
                .orElse(null);

        if (leaderInfo == null) return false;

        List<IPlayerClaimPosListAPI> allLists = leaderInfo.getDimension(dimension.getValue()).getStream().toList();
        Set<ChunkPos> allClaims = new HashSet<>();

        for (IPlayerClaimPosListAPI list : allLists) {
            list.getStream().forEach(allClaims::add);
        }

        // Remove the chunk being unclaimed
        allClaims.remove(toUnclaim);

        if (allClaims.isEmpty()) {
            return true; // If no claims remain, it's trivially connected
        }

        // BFS from one remaining chunk
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

        return visited.size() != allClaims.size(); // disconnected if BFS didn't reach all
    }

    public static boolean isAdjacentToExistingClaim(
            IServerParty<?, ?, ?> party,
            RegistryKey<World> dimension,
            ChunkPos targetChunk,
            IServerClaimsManager<IPlayerChunkClaim, IServerPlayerClaimInfo<IPlayerDimensionClaims<IPlayerClaimPosList>>, IServerDimensionClaimsManager<IServerRegionClaims>> claimsManager
    ) {
        IServerPlayerClaimInfoAPI leaderInfo = claimsManager.getPlayerInfoStream()
                .filter(e -> e.getPlayerId().equals(party.getOwner().getUUID()))
                .findFirst()
                .orElse(null);

        if (leaderInfo == null) return false;

        List<IPlayerClaimPosListAPI> allLists = leaderInfo.getDimension(dimension.getValue()).getStream().toList();
        Set<ChunkPos> allClaims = new HashSet<>();

        for (IPlayerClaimPosListAPI list : allLists) {
            list.getStream().forEach(allClaims::add);
        }

        // First claim is always allowed
        if (allClaims.isEmpty()) {
            return true;
        }

        // Check for adjacency
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
