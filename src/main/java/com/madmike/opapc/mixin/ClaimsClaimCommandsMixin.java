package com.madmike.opapc.mixin;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.command.ClaimsClaimCommands;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import net.minecraft.command.CommandSource;

@Mixin(ClaimsClaimCommands.class)
public class ClaimsClaimCommandsMixin {

    /**
     * Overwrites the existing createClaimCommand method to include a party leader check.
     */
    @Overwrite
    protected static ArgumentBuilder<CommandSourceStack, ?> createClaimCommand(ArgumentBuilder<CommandSourceStack, ?> builder, boolean shouldClaim, boolean serverClaim, boolean opReplaceCurrent) {
        return builder.executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();

            // ✅ Enforce "party leader only" restriction
            IServerPartyAPI party = OpenPACServerAPI.get(player.getServer()).getPartyManager().getPartyByMember(player.getUUID());
            if (party == null || !party.getLeader().equals(player.getUUID())) {
                player.sendSystemMessage(Component.literal("Only party leaders can " + (shouldClaim ? "claim" : "unclaim") + " land.").withStyle(ChatFormatting.RED));
                return 0;
            }

            // Fallback to original command logic
            return Commands.literal("claim").build().getCommand().run(context); // This just redirects; you'll likely paste in full logic instead
        });
    }
}
