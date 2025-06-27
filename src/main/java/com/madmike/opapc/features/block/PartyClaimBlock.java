package com.madmike.opapc.features.block;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.data.parties.claims.Donor;
import com.madmike.opapc.data.parties.claims.PartyClaim;
import com.madmike.opapc.features.entity.PartyClaimBlockEntity;
import com.madmike.opapc.util.CurrencyUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public class PartyClaimBlock extends BlockWithEntity {

    public PartyClaimBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PartyClaimBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) {
            player.sendMessage(Text.literal("Hold currency to donate."), false);
            return ActionResult.FAIL;
        }

        // Try get bronze value of item
        long value = CurrencyUtil.getValueOfItemStack(stack);
        if (value <= 0) {
            player.sendMessage(Text.literal("This item has no donation value."), false);
            return ActionResult.FAIL;
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof PartyClaimBlockEntity claimEntity)) {
            player.sendMessage(Text.literal("Invalid claim block."), false);
            return ActionResult.FAIL;
        }

        UUID partyId = claimEntity.getPartyId();
        if (partyId == null) {
            player.sendMessage(Text.literal("This claim block isn't assigned to a party."), false);
            return ActionResult.FAIL;
        }

        PartyClaim claim = OPAPCComponents.PARTY_CLAIMS.get(player.getScoreboard()).getClaim(partyId);
        claim.addDonation(player.getUuid(), player.getName().getString(), value);
        return ActionResult.CONSUME;
    }


    // Floating Donation Text Stuff

    private static final String TAG = "donation_text";
    private static final BlockPos CHEST_POS = new BlockPos(100, 64, 100); // Adjust as needed
    private static final double BASE_HEIGHT = 2.0;
    private static final double LINE_SPACING = 0.25;

    public static void updateLeaderboardText(ServerWorld world, List<Donor> top3) {
        // Remove existing text displays nearby
        Box searchBox = new Box(CHEST_POS).expand(1.5).stretch(0, 4, 0);
        world.getEntitiesByType(EntityType.TEXT_DISPLAY, searchBox, e -> e.getCommandTags().contains(TAG)).forEach(e -> e.discard());

        // Spawn updated text displays
        for (int i = 0; i < top3.size(); i++) {
            Donor entry = top3.get(i);

            DisplayEntity.TextDisplayEntity textDisplay = new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
            Vec3d pos = Vec3d.ofCenter(CHEST_POS).add(0, BASE_HEIGHT + i * LINE_SPACING, 0);
            textDisplay.setPosition(pos);
            // textDisplay.setBillboardMode(DisplayEntity.BillboardMode.FIXED);
            //textDisplay.setShadowed(false);
            //textDisplay.setSeeThrough(true);
            textDisplay.setCustomNameVisible(true);
            textDisplay.setCustomName(formatDonatorText(i + 1, entry));
            textDisplay.addCommandTag(TAG);
            world.spawnEntity(textDisplay);
        }
    }

    private static Text formatDonatorText(int rank, Donor entry) {
        return Text.literal(rank + ". " + entry.name() + " - " + entry.amount() + "g")
                .copy().styled(style -> switch (rank) {
                    case 1 -> style.withColor(0xFFD700).withBold(true); // Gold
                    case 2 -> style.withColor(0xAAAAAA).withBold(true); // Gray
                    case 3 -> style.withColor(0x555555).withBold(true); // Dark Gray
                    default -> style.withColor(0xFFFFFF);
                });
    }
}
