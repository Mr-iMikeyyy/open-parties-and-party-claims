package com.madmike.opapc.mixin;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.components.player.PartyRejoinCooldownComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.pac.common.parties.party.Party;
import xaero.pac.common.parties.party.member.PartyInvite;

import java.util.UUID;

@Mixin(Party.class)
public abstract class PartyInviteMixin {

    /**
     * Blocks rejoining via the normal invite method.
     */
    @Inject(
            method = "invitePlayer(Ljava/util/UUID;Ljava/lang/String;)Lxaero/pac/common/parties/party/member/PartyInvite;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onInvitePlayer(UUID playerUUID, String playerUsername, CallbackInfoReturnable<PartyInvite> cir) {
        checkCooldown((Party)(Object)this, playerUUID, playerUsername, cir);
    }

    /**
     * Blocks rejoining via the "clean" invite method (used internally by OPAC).
     */
    @Inject(
            method = "invitePlayerClean(Ljava/util/UUID;Ljava/lang/String;)Lxaero/pac/common/parties/party/member/PartyInvite;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onInvitePlayerClean(UUID playerUUID, String playerUsername, CallbackInfoReturnable<PartyInvite> cir) {
        checkCooldown((Party)(Object)this, playerUUID, playerUsername, cir);
    }

    private void checkCooldown(Party party, UUID playerUUID, String playerUsername, CallbackInfoReturnable<PartyInvite> cir) {
        UUID partyId = party.getId();

        // Look up target player
        ServerPlayer targetPlayer = OPAPC.getServer().getPlayerList().getPlayer(playerUUID);
        if (targetPlayer == null) return; // offline

        PartyRejoinCooldownComponent cooldown = OPAPCComponents.PARTY_REJOIN_COOLDOWN.get(targetPlayer);

        if (!cooldown.canPlayerRejoinParty(partyId)) {
            String timeLeft = cooldown.getRemainingTimeFormatted(partyId);

            // Message inviter if online
            ServerPlayer inviter = OPAPC.getServer().getPlayerList().getPlayer(party.getOwner().getUUID());
            if (inviter != null) {
                inviter.sendSystemMessage(Component.literal(
                        playerUsername + " cannot rejoin this party for another " + timeLeft
                ));
            }

            // Optional: also message the target
            targetPlayer.sendSystemMessage(Component.literal(
                    "You cannot rejoin " + party.getDefaultName() + " for another " + timeLeft
            ));

            cir.setReturnValue(null);
        }
    }
}
