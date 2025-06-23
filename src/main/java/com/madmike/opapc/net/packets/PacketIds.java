package com.madmike.opapc.net.packets;

import com.madmike.opapc.OPAPC;
import net.minecraft.util.Identifier;

public class PacketIds {
    public static final Identifier REMOVE_OFFER = new Identifier(OPAPC.MOD_ID, "remove_offer");
    public static final Identifier BUY_OFFER = new Identifier(OPAPC.MOD_ID, "buy_offer");

    public static final Identifier REFRESH_TRADE_SCREEN = new Identifier(OPAPC.MOD_ID, "refresh_trades");
    public static final Identifier REBUILD_TABS = new Identifier(OPAPC.MOD_ID, "rebuild_tabs");
}
