package com.madmike.opapc;

import com.madmike.opapc.command.CommandsManager;
import com.madmike.opapc.event.EventManager;
import com.madmike.opapc.net.ServerReceiver;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OPAPC implements ModInitializer {
	public static final String MOD_ID = "opapc";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing OPATR...");

		//Register Events
		EventManager.register();

		// Register commands
		CommandsManager.registerCommands();

		// Register this server-side
		ServerReceiver.register();

		LOGGER.info("OPATR Initialized! Happy Trading!");
	}
}