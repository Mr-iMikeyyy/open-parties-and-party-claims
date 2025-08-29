/*
 * Copyright (C) 2025 Mr-iMikeyyy (and contributors)
 *
 * This file is part of OPAPC (Open Parties and Party Claims).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Lesser General Public License
 * (LGPL-3.0-only) as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.madmike.opapc;

import com.madmike.opapc.bounty.events.BountyEvents;
import com.madmike.opapc.duel.DuelBannedItems;
import com.madmike.opapc.duel.event.DuelEvents;
import com.madmike.opapc.partyclaim.command.PartyClaimCommand;
import com.madmike.opapc.player.event.PlayerEvents;
import com.madmike.opapc.raid.command.RaidCommand;
import com.madmike.opapc.raid.events.RaidEvents;
import com.madmike.opapc.trade.events.TradeEvents;
import com.madmike.opapc.war.event.WarEvents;
import com.madmike.opapc.war.features.WarFeatures;
import com.madmike.opapc.trade.net.TradeServerReceiver;
import com.madmike.opapc.war.command.WarCommand;
import com.madmike.opapc.warp.command.WarpCommand;
import com.madmike.opapc.warp.events.WarpEvents;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.player.config.api.IPlayerConfigManagerAPI;

public class OPAPC implements ModInitializer {
	public static final String MOD_ID = "opapc";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static MinecraftServer server;

	public static @Nullable MinecraftServer getServer() { return server; }

	public static MinecraftServer requireServer() {
		if (server == null) throw new IllegalStateException("Server not available yet");
		return server;
	}

	public static Scoreboard scoreboard() {
		return requireServer().getScoreboard();
	}

	public static IPartyManagerAPI parties() {
		return OpenPACServerAPI.get(requireServer()).getPartyManager();
	}

	public static IServerClaimsManagerAPI claims() {
		return OpenPACServerAPI.get(requireServer()).getServerClaimsManager();
	}

	public static IPlayerConfigManagerAPI playerConfigs() {
		return OpenPACServerAPI.get(requireServer()).getPlayerConfigs();
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing OPATR...");

		// Register to capture the server on start
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			server = s;
		});

		// Clear reference when the server stops to avoid leaks
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			server = null;
		});

		//Load Config
		OPAPCConfig.load();

		//Register Events
		PlayerEvents.register();
		BountyEvents.register();
		DeathMatchEvents.register();
		DuelEvents.register();
		RaidEvents.register();
		TradeEvents.register();
		WarEvents.register();
		WarpEvents.register();

		// Register commands
		PartyClaimCommand.register();
		WarCommand.register();
		WarpCommand.register();
		RaidCommand.register();

		// Register Packet Receivers
		TradeServerReceiver.register();

		//Register Items and Blocks
		WarFeatures.register();

		LOGGER.info("OPAPC Initialized. Good luck out there!");
	}

	public static void broadcast(Component msg) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.sendSystemMessage(msg);
		}
	}
}