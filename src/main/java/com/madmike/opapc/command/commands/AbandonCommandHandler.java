package com.madmike.opapc.command.commands;

import com.madmike.opapc.components.OPAPCComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
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

        info.getDimension(World.OVERWORLD.getValue()).getStream().forEach(e -> e.getStream().forEach(overworldClaims::add));
        info.getDimension(World.NETHER.getValue()).getStream().forEach(e -> e.getStream().forEach(netherClaims::add));

        for (ChunkPos pos : overworldClaims) {
            cm.unclaim(World.OVERWORLD.getValue(), pos.x, pos.z);
        }

        for (ChunkPos pos : netherClaims) {
            cm.unclaim(World.NETHER.getValue(), pos.x, pos.z);
        }

        OPAPCComponents.PARTY_CLAIMS.get(server.getScoreboard()).removeClaim(partyId);
    }
}
