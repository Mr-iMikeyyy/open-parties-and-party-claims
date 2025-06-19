package com.madmike.opapc.permission;

import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.player.data.ServerPlayerData;
import xaero.pac.common.server.player.permission.api.IPermissionNodeAPI;
import xaero.pac.common.server.player.permission.api.IPlayerPermissionSystemAPI;
import xaero.pac.common.server.player.permission.api.UsedPermissionNodes;

import java.util.Optional;
import java.util.OptionalInt;

public class LeaderOnlyPermissionSystem implements IPlayerPermissionSystemAPI {

    @Override
    public OptionalInt getIntPermission(ServerPlayer serverPlayer, IPermissionNodeAPI<Integer> iPermissionNodeAPI) {
        return OptionalInt.empty();
    }

    @Override
    public boolean getPermission(ServerPlayerEntity player, IPermissionNodeAPI<Boolean> iPermissionNodeAPI) {
        if (node == UsedPermissionNodes..CLAIM_ALLOWED || node == UsedPermissionNodes.UNCLAIM_ALLOWED) {
            var partyManager = OpenPACServerAPI.get(player.getServer()).getPartyManager();
            var party = partyManager.getPartyByMember(player.getUuid());
            return party != null && party.getOwner().getUUID().equals(player.getUuid());
        }

        // Allow all other permissions by default
        return true;
    }

    @Override
    public <T> Optional<T> getPermissionTyped(ServerPlayerEntity serverPlayerEntity, IPermissionNodeAPI<T> iPermissionNodeAPI) {
        return Optional.empty();
    }
}
