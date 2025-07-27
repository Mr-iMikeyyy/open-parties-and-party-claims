package com.madmike.opapc.command.commands.claims;

import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.claims.result.api.ClaimResult;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;

import static com.madmike.opapc.util.NetherClaimAdjuster.mirrorOverworldClaimsToNether;

public class PartyUnclaimCommandHandler {
    public static int handlePartyUnclaimCommand(IServerClaimsManagerAPI cm, ServerPlayer player, PartyClaim pc) {


        // All checks passed, try to unclaim!
        ClaimResult<IPlayerChunkClaimAPI> result = cm.tryToUnclaim(Level.OVERWORLD.location(), player.getUUID(), player.chunkPosition().x, player.chunkPosition().z, player.chunkPosition().x, player.chunkPosition().z, false);

        player.sendSystemMessage(result.getResultType().message, true);

        if (result.getResultType().success) {

            if (pc.getBoughtClaims() <= 0) {
                OPAPCComponents.PARTY_CLAIMS.get(player.getScoreboard()).removeClaim(pc.getPartyId());
                return 1;
            }
            mirrorOverworldClaimsToNether(cm, player);
            return 1;
        }

        return 0;
    }
}
