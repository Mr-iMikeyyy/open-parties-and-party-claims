package com.madmike.opapc.command.commands.claims;

import com.madmike.opapc.components.OPAPCComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaero.pac.common.claims.player.api.IPlayerClaimInfoAPI;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AbandonCommandHandler {

    public static void handleAbandonCommand(IPartyMemberAPI owner, UUID partyId, OpenPACServerAPI api, MinecraftServer server) {
        IServerClaimsManagerAPI cm = api.getServerClaimsManager();
        IPlayerClaimInfoAPI info = cm.getPlayerInfo(owner.getUUID());

        List<ChunkPos> overworldClaims = new ArrayList<>();
        List<ChunkPos> netherClaims = new ArrayList<>();

        ResourceKey<Level> overworldKey = Level.OVERWORLD;
        ResourceKey<Level> netherKey = Level.NETHER;

        info.getDimension(overworldKey.location()).getStream().forEach(e -> e.getStream().forEach(overworldClaims::add));
        info.getDimension(netherKey.location()).getStream().forEach(e -> e.getStream().forEach(netherClaims::add));

        for (ChunkPos pos : overworldClaims) {
            cm.unclaim(overworldKey.location(), pos.x, pos.z);
        }

        for (ChunkPos pos : netherClaims) {
            cm.unclaim(netherKey.location(), pos.x, pos.z);
        }

        OPAPCComponents.PARTY_CLAIMS.get(server.getScoreboard()).removeClaim(partyId);
    }
}
