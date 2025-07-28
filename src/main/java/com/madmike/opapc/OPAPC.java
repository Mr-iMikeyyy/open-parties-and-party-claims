package com.madmike.opapc;

import com.madmike.opapc.bounty.events.BountyEvents;
import com.madmike.opapc.command.CommandsManager;
import com.madmike.opapc.duel.events.DuelEvents;
import com.madmike.opapc.partyclaim.command.PartyClaimCommand;
import com.madmike.opapc.raid.command.RaidCommand;
import com.madmike.opapc.raid.events.RaidEvents;
import com.madmike.opapc.trade.events.TradeEvents;
import com.madmike.opapc.war.features.WarFeatures;
import com.madmike.opapc.trade.packets.TradeServerReceiver;
import com.madmike.opapc.war.command.WarCommand;
import com.madmike.opapc.war.events.WarEvents;
import com.madmike.opapc.warp.command.WarpCommand;
import com.madmike.opapc.warp.events.WarpEvents;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
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
	private static IPartyManagerAPI partyManager;
	private static IServerClaimsManagerAPI claimsManager;
	private static IPlayerConfigManagerAPI playerConfigs;

	public static MinecraftServer getServer() {
		return server;
	}
	public static IPartyManagerAPI getPartyManager() {
		return partyManager;
	}
	public static IServerClaimsManagerAPI getClaimsManager() {
		return claimsManager;
	}
	public static IPlayerConfigManagerAPI getPlayerConfigs() {
		return playerConfigs;
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing OPATR...");

		// Register to capture the server on start
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			server = s;
			partyManager = OpenPACServerAPI.get(server).getPartyManager();
			claimsManager = OpenPACServerAPI.get(server).getServerClaimsManager();
			playerConfigs = OpenPACServerAPI.get(server).getPlayerConfigs();
		});

		// Clear reference when the server stops to avoid leaks
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			server = null;
			partyManager = null;
			claimsManager = null;
			playerConfigs = null;
		});

		//Register Events
		BountyEvents.register();
		DeathMatchEvents.register();
		DuelEvents.register();
		RaidEvents.register();
		TradeEvents.register();
		WarEvents.register();
		WarpEvents.register();

		// Register commands
		PartyClaimCommand.register();
		CommandsManager.registerCommands();
		WarCommand.register();
		WarpCommand.register();
		RaidCommand.register();

		// Register Packet Receivers
		TradeServerReceiver.register();

		//Register Items and Blocks
		WarFeatures.register();

		LOGGER.info("OPAPC Initialized. Good luck out there!");
	}
}