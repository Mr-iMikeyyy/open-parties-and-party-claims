package com.madmike.opapc.features.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;

import java.util.*;

public class PartyClaimBlock extends Block {

    public PartyClaimBlock(Properties properties) {
        super(properties);
    }

    private static final Set<BlockPos> IGNORE_UNCLAIM = Collections.synchronizedSet(new HashSet<>());

    // When you want to remove block without unclaiming:
    public static void removeWithoutUnclaim(ServerLevel level, BlockPos pos) {
        IGNORE_UNCLAIM.add(pos.immutable());
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);

        if (!level.isClientSide && !state.is(newState.getBlock())) {
            if (!IGNORE_UNCLAIM.remove(pos)) {
                // Only unclaim if not in ignore set!
                unclaimChunk((ServerLevel)level, pos);
            }
        }
    }

    private void unclaimChunk(ServerLevel level, BlockPos pos) {
        IServerClaimsManagerAPI cm = OpenPACServerAPI.get(level.getServer()).getServerClaimsManager();
        cm.unclaim(Level.OVERWORLD.location(), level.getChunkAt(pos).getPos().x, level.getChunkAt(pos).getPos().z);
    }
}
