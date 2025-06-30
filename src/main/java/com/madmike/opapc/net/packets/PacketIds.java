package com.madmike.opapc.net.packets;

import com.madmike.opapc.OPAPC;
import net.minecraft.resources.ResourceLocation;

public class PacketIds {

    public static final ResourceLocation REMOVE_OFFER = new ResourceLocation(OPAPC.MOD_ID, "remove_offer");
    public static final ResourceLocation BUY_OFFER = new ResourceLocation(OPAPC.MOD_ID, "buy_offer");

    public static final ResourceLocation REFRESH_TRADE_SCREEN = new ResourceLocation(OPAPC.MOD_ID, "refresh_trades");
    public static final ResourceLocation REBUILD_TABS = new ResourceLocation(OPAPC.MOD_ID, "rebuild_tabs");
}
