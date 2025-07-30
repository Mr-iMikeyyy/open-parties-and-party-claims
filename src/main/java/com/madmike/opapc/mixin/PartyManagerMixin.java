package com.madmike.opapc.mixin;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.partyclaim.components.scoreboard.PartyClaimsComponent;
import com.madmike.opapc.trade.components.scoreboard.OffersComponent;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.util.NetherClaimAdjuster;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.pac.common.claims.player.api.IPlayerClaimPosListAPI;
import xaero.pac.common.parties.party.member.PartyMember;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.parties.party.PartyManager;
import xaero.pac.common.server.parties.party.ServerParty;

import java.util.List;
import java.util.UUID;

@Mixin(PartyManager.class)
public abstract class PartyManagerMixin {

    @SuppressWarnings("target")
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "onMemberAdded", at = @At("TAIL"), remap = false)
    private void onMemberAdded(ServerParty party, PartyMember member, CallbackInfo ci) {
        if (party != null && member != null) {
            OPAPCComponents.OFFERS.get(server.getScoreboard())
                    .updatePartyForPlayer(member.getUUID(), party.getId());
        }
    }

    @Inject(method = "onMemberRemoved", at = @At("TAIL"), remap = false)
    private void onMemberRemoved(ServerParty party, PartyMember member, CallbackInfo ci) {
        if (party != null && member != null) {
            OPAPCComponents.OFFERS.get(server.getScoreboard()).updatePartyForPlayer(member.getUUID(), null);
            OPAPCComponents.PARTY_REJOIN_COOLDOWN.get(OPAPC.getServer().getScoreboard()).onPartyLeave(party.getId());
        }
    }

    @Inject(method = "onOwnerChange", at = @At("TAIL"), remap = false)
    private void onOwnerChange(PartyMember oldOwner, PartyMember newOwner, CallbackInfo ci) {
        if (oldOwner != null && newOwner != null) {
            IServerClaimsManagerAPI claimManager = OPAPC.getClaimsManager();

            List<ChunkPos> chunksToTransfer = claimManager
                    .getPlayerInfo(oldOwner.getUUID())
                    .getDimension(Level.OVERWORLD.location())
                    .getStream()
                    .flatMap(IPlayerClaimPosListAPI::getStream)
                    .toList();

            for (ChunkPos chunk : chunksToTransfer) {
                claimManager.unclaim(Level.OVERWORLD.location(), chunk.x, chunk.z);
                claimManager.claim(Level.OVERWORLD.location(), newOwner.getUUID(), chunk.x, chunk.z, 0, false);
            }
        }
    }

    @Inject(method = "createPartyForOwner", at = @At("TAIL"), remap = false)
    private void createPartyForOwner(Player owner, CallbackInfoReturnable<ServerParty> cir) {
        ServerParty created = cir.getReturnValue();
        if (created != null) {
            OPAPCComponents.OFFERS.get(OPAPC.getServer().getScoreboard()).updatePartyForPlayer(owner.getUUID(), created.getId());
        }
    }

    @Inject(method = "removeTypedParty", at = @At("TAIL"), remap = false)
    private void onRemoveTypedParty(ServerParty party, CallbackInfo ci) {
        if (party != null) {

            PartyClaimsComponent comp = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard());

            List<ChunkPos> claimedChunks = comp.getClaim(party.getId()).getClaimedChunksList();

            for (ChunkPos chunk : claimedChunks) {
                OPAPC.getClaimsManager().unclaim(Level.OVERWORLD.location(), chunk.x, chunk.z);
            }

            NetherClaimAdjuster.mirrorOverworldClaimsToNether(party.getOwner().getUUID());

            comp.removeClaim(party.getId());

            OffersComponent offers = OPAPCComponents.OFFERS.get(server.getScoreboard());
            List<UUID> memberIds = party.getMemberInfoStream()
                    .map(IPartyMemberAPI::getUUID)
                    .toList();

            for (UUID id : memberIds) {
                offers.updatePartyForPlayer(id, null);
            }
        }
    }
}
