package com.madmike.opapc.features;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.features.block.PartyClaimBlock;
import com.madmike.opapc.features.entity.PartyClaimBlockEntity;
import com.madmike.opapc.features.item.PartyClaimBlockItem;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;


public class OPAPCFeatures {

    public static final Block PARTY_CLAIM_BLOCK = new PartyClaimBlock(FabricBlockSettings.of().strength(-1.0f, 3600000.0f).noLootTable());
    public static final Item PARTY_CLAIM_BLOCK_ITEM = new PartyClaimBlockItem(PARTY_CLAIM_BLOCK, new Item.Properties());
    public static final BlockEntityType<PartyClaimBlockEntity> PARTY_CLAIM_BLOCK_ENTITY =
            FabricBlockEntityTypeBuilder.create(PartyClaimBlockEntity::new, PARTY_CLAIM_BLOCK).build();

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(OPAPC.MOD_ID, "party_claim_block"), PARTY_CLAIM_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(OPAPC.MOD_ID, "party_claim_block"), PARTY_CLAIM_BLOCK_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation(OPAPC.MOD_ID, "party_claim_block_entity"), PARTY_CLAIM_BLOCK_ENTITY);
    }
}
