package com.madmike.opapc;

import com.madmike.opapc.command.CommandsManager;
import com.madmike.opapc.event.EventManager;
import com.madmike.opapc.features.OPAPCFeatures;
import com.madmike.opapc.trade.packets.TradeServerReceiver;
import com.madmike.opapc.war.WarCommand;
import com.madmike.opapc.war.WarEvents;
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

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
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
		EventManager.register();
		WarEvents.register();

		// Register commands
		CommandsManager.registerCommands();
		WarCommand.registerWarCommand();

		// Register Server Packet Receiver
		TradeServerReceiver.register();

		OPAPCFeatures.register();

		LOGGER.info("OPAPC Initialized. Good luck out there!");
	}
}