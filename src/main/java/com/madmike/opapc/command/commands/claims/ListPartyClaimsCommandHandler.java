package com.madmike.opapc.command.commands.claims;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.components.scoreboard.parties.PartyClaimsComponent;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class ListPartyClaimsCommandHandler {
    public static void handleListPartyClaimsCommand(CommandContext<CommandSourceStack> ctx) {

        PartyClaimsComponent comp = OPAPCComponents.PARTY_CLAIMS.get(ctx.getSource().getServer().getScoreboard());
        var claims = comp.getAllClaims();

        if (claims.isEmpty()) {
            ctx.getSource().sendSuccess(() ->
                    Component.literal("No party claims exist."), false);
        }

        ctx.getSource().sendSuccess(() ->
                Component.literal("=== Party Claims Breakdown ==="), false);

        for (var entry : claims.entrySet()) {
            var partyId = entry.getKey();
            var claim = entry.getValue();
            var boughtClaims = claim.getBoughtClaims();

            ctx.getSource().sendSuccess(() ->
                    Component.literal(String.format(
                            "Party: %s | Bought Claims: %d",
                            partyId, boughtClaims
                    )), false);
        }
    }
}
