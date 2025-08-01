package com.madmike.opapc.war2.command.util;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.war2.War;
import com.madmike.opapc.war2.WarManager2;
import com.madmike.opapc.war2.data.WarData2;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.List;

public class WarValidator {
    public static WarValidationResult validateDeclaration(
            IServerPartyAPI attackingParty,
            IServerPartyAPI defendingParty
    ) {
        if (attackingParty == null) {
            return WarValidationResult.fail("You must own a party to declare a war.");
        }

        var comp = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard());
        PartyClaim attackingClaim = comp.getClaim(attackingParty.getId());
        if (attackingClaim == null) {
            return WarValidationResult.fail("Your party must own a claim to declare war.");
        }

        PartyClaim defendingClaim = comp.getClaim(defendingParty.getId());
        if (defendingClaim == null) {
            return WarValidationResult.fail("That party does not own a claim.");
        }

        if (attackingParty.isAlly(defendingParty.getId())) {
            return WarValidationResult.fail("You cannot declare war on your allies.");
        }

        if (defendingClaim.isWarInsured()) {
            return WarValidationResult.fail("That party is currently insured against wars.");
        }

        // Check active wars
        for (War war : WarManager2.INSTANCE.getActiveWars()) {
            WarData2 data = war.getData();
            if (data.getAttackingParty().getId().equals(attackingParty.getId())
                    || data.getDefendingParty().getId().equals(attackingParty.getId())) {
                return WarValidationResult.fail("You are already in a war!");
            }
            if (data.getDefendingParty().getId().equals(defendingParty.getId())
                    || data.getAttackingParty().getId().equals(defendingParty.getId())) {
                return WarValidationResult.fail("This party is already in a war!");
            }
        }

        // Check online defenders
        List<ServerPlayer> defenders = defendingParty.getOnlineMemberStream().toList();
        if (defenders.isEmpty()) {
            return WarValidationResult.fail("There's no one online to defend that claim.");
        }

        return WarValidationResult.success();
    }
}
