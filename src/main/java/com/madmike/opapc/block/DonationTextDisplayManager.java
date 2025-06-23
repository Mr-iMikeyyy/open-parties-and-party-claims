package com.madmike.opapc.block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class DonationTextDisplayManager {

    private static final String TAG = "donation_text";
    private static final BlockPos CHEST_POS = new BlockPos(100, 64, 100); // Adjust as needed
    private static final double BASE_HEIGHT = 2.0;
    private static final double LINE_SPACING = 0.25;

    public static void updateLeaderboardText(ServerWorld world, List<DonatorEntry> top3) {
        // Remove existing text displays nearby
        Box searchBox = new Box(CHEST_POS).expand(1.5).stretch(0, 4, 0);
        world.getEntitiesByType(EntityType.TEXT_DISPLAY, searchBox, e -> e.getCommandTags().contains(TAG)).forEach(e -> e.discard());

        // Spawn updated text displays
        for (int i = 0; i < top3.size(); i++) {
            DonatorEntry entry = top3.get(i);

            TextDisplayEntity textDisplay = new TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
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

    private static Text formatDonatorText(int rank, DonatorEntry entry) {
        return Text.literal(rank + ". " + entry.name() + " - " + entry.amount() + "g")
                .copy().styled(style -> {
                    return switch (rank) {
                        case 1 -> style.withColor(0xFFD700).withBold(true); // Gold
                        case 2 -> style.withColor(0xAAAAAA).withBold(true); // Gray
                        case 3 -> style.withColor(0x555555).withBold(true); // Dark Gray
                        default -> style.withColor(0xFFFFFF);
                    };
                });
    }

    public record DonatorEntry(String name, long amount) {}
}
