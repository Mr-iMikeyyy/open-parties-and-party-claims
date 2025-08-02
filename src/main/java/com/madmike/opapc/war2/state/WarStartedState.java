package com.madmike.opapc.war2.state;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.util.SafeWarpFinder;
import com.madmike.opapc.war2.EndOfWarType;
import com.madmike.opapc.war2.War;
import com.madmike.opapc.war2.data.WarData2;
import com.madmike.opapc.war2.event.bus.WarEventBus;
import com.madmike.opapc.war2.event.events.WarEndedEvent;
import com.madmike.opapc.war2.features.block.WarBlockSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class WarStartedState implements IWarState {

    @Override
    public void tick(War war) {
        if (war.getData().isExpired()) {
            war.end(EndOfWarType.ATTACKERS_LOSE_TIME);
        }
    }

    @Override
    public void onAttackerDeath(ServerPlayer player, War war) {
        player.setHealth(player.getMaxHealth());
        WarData2 data = war.getData();
        data.decrementAttackerLivesRemaining();
        if (data.getAttackerLivesRemaining() <= 0) {
            end(war, EndOfWarType.ATTACKERS_LOSE_DEATHS);
        }
        else {
            BlockPos safeWarpPos = SafeWarpFinder.findSafeSpawnOutsideClaim(war.getData().getDefendingClaim());
            if (safeWarpPos != null) {
                player.teleportTo(OPAPC.getServer().overworld(), safeWarpPos.getX() + 0.5, safeWarpPos.getY(), safeWarpPos.getZ() + 0.5, player.getYRot(), player.getXRot());
            }
            else {
                end(war, EndOfWarType.BUG);
            }
        }
    }

    @Override
    public void onDefenderDeath(ServerPlayer player, War war) {
        player.setHealth(player.getMaxHealth());
        BlockPos safeSpawnPos = SafeWarpFinder.findSafeSpawnInsideClaim(war.getData().getDefendingClaim());
        if (safeSpawnPos != null) {
            player.teleportTo(OPAPC.getServer().overworld(), safeSpawnPos.getX(), safeSpawnPos.getY(), safeSpawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
        }
        else if (war.getData().getDefendingClaim().getWarpPos() != null){
            BlockPos partyWarpPos = war.getData().getDefendingClaim().getWarpPos();
            player.teleportTo(OPAPC.getServer().overworld(), partyWarpPos.getX() + 0.5, partyWarpPos.getY(), partyWarpPos.getZ() + 0.5, player.getYRot(), player.getXRot());
        }
        else {
            end(war, EndOfWarType.BUG);
        }
    }

    @Override
    public void onWarBlockBroken(BlockPos pos, War war) {
        WarData2 data = war.getData();
        war.getData().decrementWarBlocksLeft();
        if (war.getData().getWarBlocksLeft() <= 0) {
            war.end(EndOfWarType.ATTACKERS_WIN_BLOCKS);
        }
        else {
            BlockPos safeSpawnPos = WarBlockSpawner.findSafeSpawn(data);
            if (safeSpawnPos != null) {
                WarBlockSpawner.spawnWarBlock(safeSpawnPos);
            }
            else {
                end(war, EndOfWarType.BUG);
            }
        }
    }

    @Override
    public void end(War war, EndOfWarType type) {
        war.setState(new WarEndedState(type));
        WarEventBus.post(new WarEndedEvent(war, type));
    }
}
