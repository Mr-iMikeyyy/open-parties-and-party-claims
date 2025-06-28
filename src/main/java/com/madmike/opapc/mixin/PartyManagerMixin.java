package com.madmike.opapc.mixin;

import com.madmike.opapc.components.scoreboard.trades.OffersComponent;
import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.data.parties.PartyName;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
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

import static com.madmike.opapc.command.commands.claims.AbandonCommandHandler.handleAbandonCommand;

@Mixin(PartyManager.class)
public abstract class PartyManagerMixin {

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "onMemberAdded", at = @At("TAIL"))
    private void onMemberAdded(ServerParty party, PartyMember m, CallbackInfo ci) {
        if (party != null && m != null) {
            OPAPCComponents.OFFERS.get(server.getScoreboard()).updatePartyForPlayer(m.getUUID(), party.getId());
        }
    }

    @Inject(method = "onMemberRemoved", at = @At("TAIL"))
    private void onMemberRemoved(ServerParty party, PartyMember m, CallbackInfo ci) {
        if (party != null && m != null) {
            OPAPCComponents.OFFERS.get(server.getScoreboard()).updatePartyForPlayer(m.getUUID(), null);
        }
    }

    @Inject(method = "onOwnerChange", at = @At("TAIL"))
    private void onOwnerChange(PartyMember oldOwner, PartyMember newOwner, CallbackInfo ci) {
        if (oldOwner != null && newOwner != null) {
            OpenPACServerAPI api = OpenPACServerAPI.get(server);
            IServerClaimsManagerAPI scm = api.getServerClaimsManager();
            List<ChunkPos> chunksToTransfer = scm
                    .getPlayerInfo(oldOwner.getUUID())
                    .getDimension(World.OVERWORLD.getValue())
                    .getStream()
                    .flatMap(IPlayerClaimPosListAPI::getStream) // IPlayerClaimPosListAPI -> Stream<ChunkPos>
                    .toList();

            for (ChunkPos chunk : chunksToTransfer) {
                scm.unclaim(World.OVERWORLD.getValue(), chunk.x, chunk.z);
                scm.claim(World.OVERWORLD.getValue(), newOwner.getUUID(), chunk.x, chunk.z, 0, false);
            }

        }
    }

    @Inject(method = "createPartyForOwner", at = @At("TAIL"))
    private void createPartyForOwner(ServerPlayerEntity owner, CallbackInfoReturnable<ServerParty> cir) {
        ServerParty created = cir.getReturnValue();
        if (created != null) {
            OPAPCComponents.PARTY_NAMES.get(server.getScoreboard()).addOrUpdatePartyName(new PartyName(created.getId(), created.getDefaultName()));
        }
    }

    @Inject(method = "removeTypedParty", at = @At("TAIL"))
    private void onRemoveTypedParty(ServerParty party, CallbackInfo ci) {
        if (party != null) {
            OpenPACServerAPI api = OpenPACServerAPI.get(server);

            handleAbandonCommand(party.getOwner(), party.getId(), api, server);

            OffersComponent comp = OPAPCComponents.OFFERS.get(server.getScoreboard());
            List<UUID> idList = party.getMemberInfoStream().map(IPartyMemberAPI::getUUID).toList();

            for (UUID id : idList) {
                comp.updatePartyForPlayer(id, null);
            }
        }
    }
}
