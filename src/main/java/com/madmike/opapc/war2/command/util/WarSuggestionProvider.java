package com.madmike.opapc.war2.command.util;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

public class WarSuggestionProvider {
    public static CompletableFuture<Suggestions> suggestTargets(
            CommandContext<?> context, SuggestionsBuilder builder) {

        ServerPlayer player;
        try {
            player = ((net.minecraft.commands.CommandSourceStack) context.getSource()).getPlayer();
        } catch (Exception e) {
            return builder.buildFuture();
        }

        if (player == null) {
            return builder.buildFuture();
        }

        IPartyManagerAPI pm = OPAPC.getPartyManager();
        IServerPartyAPI attackingParty = pm.getPartyByOwner(player.getUUID());
        if (attackingParty == null) {
            return builder.buildFuture();
        }

        var comp = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard());
        PartyClaim attackingClaim = comp.getClaim(attackingParty.getId());
        if (attackingClaim == null) {
            return builder.buildFuture();
        }

        for (PartyClaim claim : comp.getAllClaims()) {
            if (claim.getPartyId().equals(attackingClaim.getPartyId())) continue;
            if (attackingParty.isAlly(claim.getPartyId())) continue;
            if (claim.isWarInsured()) continue;
            builder.suggest(claim.getPartyName());
        }

        return builder.buildFuture();
    }

}
