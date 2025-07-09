package com.madmike.opapc.command.commands.claims;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.claims.result.api.ClaimResult;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;

import static com.madmike.opapc.util.NetherClaimAdjuster.mirrorOverworldClaimsToNether;

public class PartyClaimCommandHandler {
    public static int handlePartyClaimCommand(IServerClaimsManagerAPI cm, ServerPlayer player) {

        // All checks passed, try to claim!
        ClaimResult<IPlayerChunkClaimAPI> result = cm.tryToClaim(Level.OVERWORLD.location(), player.getUUID(), 0, player.chunkPosition().x, player.chunkPosition().z, player.chunkPosition().x, player.chunkPosition().z, false);

        player.sendSystemMessage(result.getResultType().message, true);

        if (result.getResultType().success) {
            mirrorOverworldClaimsToNether(cm, player);
            return 1;
        }

        return 0;
    }
}
