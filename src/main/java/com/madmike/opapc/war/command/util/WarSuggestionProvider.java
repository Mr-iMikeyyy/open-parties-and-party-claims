/*
 * Copyright (C) 2025 Mr-iMikeyyy (and contributors)
 *
 * This file is part of OPAPC (Open Parties and Party Claims).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Lesser General Public License
 * (LGPL-3.0-only) as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.madmike.opapc.war.command.util;

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
