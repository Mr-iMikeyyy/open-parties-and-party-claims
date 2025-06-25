package com.madmike.opapc.features;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.features.block.PartyClaimBlock;
import com.madmike.opapc.features.entity.PartyClaimBlockEntity;
import com.madmike.opapc.features.item.PartyClaimBlockItem;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class OPAPCFeatures {
    public static final Block PARTY_CLAIM_BLOCK = Registry.register(
            Registries.BLOCK,
            new Identifier(OPAPC.MOD_ID, "party_claim_block"),
            new PartyClaimBlock(FabricBlockSettings.create().strength(1.5f))
    );

    public static final Item PARTY_CLAIM_BLOCK_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(OPAPC.MOD_ID, "party_claim_block"),
            new PartyClaimBlockItem(PARTY_CLAIM_BLOCK, new Item.Settings())
    );

    public static final BlockEntityType<PartyClaimBlockEntity> PARTY_CLAIM = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(OPAPC.MOD_ID, "party_claim"),
            FabricBlockEntityTypeBuilder.create(PartyClaimBlockEntity::new, OPAPCFeatures.PARTY_CLAIM_BLOCK).build()
    );
}
