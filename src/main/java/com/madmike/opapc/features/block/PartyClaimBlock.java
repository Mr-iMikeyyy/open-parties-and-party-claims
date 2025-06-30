package com.madmike.opapc.features.block;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.data.parties.claims.Donor;
import com.madmike.opapc.data.parties.claims.PartyClaim;
import com.madmike.opapc.features.entity.PartyClaimBlockEntity;
import com.madmike.opapc.util.CurrencyUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xaero.pac.common.server.api.OpenPACServerAPI;

import java.util.List;
import java.util.UUID;

public class PartyClaimBlock extends Block {

    private static final String TAG = "donation_text";
    private static final BlockPos CHEST_POS = new BlockPos(100, 64, 100); // Adjust as needed
    private static final double BASE_HEIGHT = 2.0;
    private static final double LINE_SPACING = 0.25;

    public PartyClaimBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide) return;

        if (!(placer instanceof ServerPlayer serverPlayer)) return;

        MinecraftServer server = serverPlayer.getServer();
        if (server == null) return;

        var api = OpenPACServerAPI.get(server);
        var party = api.getPartyManager().getPartyByOwner(serverPlayer.getUUID());

        if (party == null) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PartyClaimBlockEntity claimBe) {
            claimBe.setPartyId(party.getId());
            claimBe.setChanged();
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("Hold currency to donate."));
            return InteractionResult.FAIL;
        }

        long value = CurrencyUtil.getValueOfItemStack(stack);
        if (value <= 0) {
            player.sendSystemMessage(Component.literal("This item has no donation value."));
            return InteractionResult.FAIL;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PartyClaimBlockEntity claimEntity)) {
            player.sendSystemMessage(Component.literal("Invalid claim block."));
            return InteractionResult.FAIL;
        }

        UUID partyId = claimEntity.getPartyId();
        if (partyId == null) {
            player.sendSystemMessage(Component.literal("This claim block isn't assigned to a party."));
            return InteractionResult.FAIL;
        }

        PartyClaim claim = OPAPCComponents.PARTY_CLAIMS.get(player.getScoreboard()).getClaim(partyId);
        claim.addDonation(player.getUUID(), player.getName().getString(), value);
        return InteractionResult.CONSUME;
    }

    public static void updateLeaderboardText(ServerLevel level, List<Donor> top3) {
        AABB searchBox = new AABB(CHEST_POS).inflate(1.5).expandTowards(0, 4, 0);
        level.getEntities(EntityType.TEXT_DISPLAY, searchBox, e -> e.getTags().contains(TAG)).forEach(e -> e.discard());

        for (int i = 0; i < top3.size(); i++) {
            Donor entry = top3.get(i);

            Display.TextDisplay textDisplay = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
            Vec3 pos = Vec3.atCenterOf(CHEST_POS).add(0, BASE_HEIGHT + i * LINE_SPACING, 0);
            textDisplay.moveTo(pos.x, pos.y, pos.z);
            textDisplay.setCustomNameVisible(true);
            textDisplay.setCustomName(formatDonatorText(i + 1, entry));
            textDisplay.addTag(TAG);
            level.addFreshEntity(textDisplay);
        }
    }

    private static Component formatDonatorText(int rank, Donor entry) {
        return Component.literal(rank + ". " + entry.name() + " - " + entry.amount() + "g")
                .copy().withStyle(style -> switch (rank) {
                    case 1 -> style.withColor(0xFFD700).withBold(true); // Gold
                    case 2 -> style.withColor(0xAAAAAA).withBold(true); // Gray
                    case 3 -> style.withColor(0x555555).withBold(true); // Dark Gray
                    default -> style.withColor(0xFFFFFF);
                });
    }
}
