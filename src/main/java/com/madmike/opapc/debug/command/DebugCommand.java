package com.madmike.opapc.debug.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class DebugCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandBuildContext, commandSelection) -> {

        });
    }
}
