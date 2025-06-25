package com.madmike.opapc.mixin;

import com.madmike.opapc.features.block.PartyClaimBlock;
import com.madmike.opapc.data.parties.claims.Donor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xaero.pac.common.parties.party.Party;

import java.util.Map;
import java.util.UUID;

@Mixin(Party.class)
public class PartyMixin {

    @Unique
    private PartyClaimBlock partyClaimBlock;

    @Unique
    public Map<UUID, Donor> getDonations() {
        return donations;
    }

    @Unique
    public void setDonations(Map<UUID, Donor> donations) {
        this.donations = donations;
    }

    @Unique
    public void addDonation(UUID)

    @Unique
    private Map<UUID, Donor> donations;

    @Unique
    public PartyClaimBlock getPartyClaimBlock() {
        return this.partyClaimBlock;
    }

    @Unique
    public void setPartyClaimBlock(PartyClaimBlock block) {
        this.partyClaimBlock = block;
    }

}
