package com.madmike.opapc.war2.features.block;

import com.madmike.opapc.war.WarManager;
import com.madmike.opapc.war2.WarManager2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class WarBlock extends Block {

    public WarBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);

        if (!level.isClientSide && level instanceof ServerLevel) {
            WarManager2.INSTANCE.handleWarBlockBroken(pos);
        }
    }
}
