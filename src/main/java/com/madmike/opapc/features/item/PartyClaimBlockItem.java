package com.madmike.opapc.features.item;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import xaero.pac.common.server.api.OpenPACServerAPI;

public class PartyClaimBlockItem extends BlockItem {
    public PartyClaimBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public ActionResult place(ItemPlacementContext context) {
        // Insert your custom logic here
        // Return ActionResult.FAIL to prevent placement
        if (context.getPlayer() instanceof ServerPlayerEntity player) {
            if (OpenPACServerAPI.get(player.getServer()).getPartyManager().getPartyByOwner(player.getUuid()) != null) {
                return super.place(context);
            }
        }
        return ActionResult.FAIL;
    }
}
