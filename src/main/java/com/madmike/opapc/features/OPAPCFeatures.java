package com.madmike.opapc.features;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.features.block.PartyClaimBlock;
import com.madmike.opapc.features.item.PartyClaimBlockItem;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;


public class OPAPCFeatures {

    public static final Block PARTY_CLAIM_BLOCK = new PartyClaimBlock(FabricBlockSettings.of().strength(50.0f, 1200.0f).noLootTable());
    public static final Item PARTY_CLAIM_BLOCK_ITEM = new PartyClaimBlockItem(PARTY_CLAIM_BLOCK, new Item.Properties());

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(OPAPC.MOD_ID, "party_claim_block"), PARTY_CLAIM_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(OPAPC.MOD_ID, "party_claim_block"), PARTY_CLAIM_BLOCK_ITEM);
    }
}
