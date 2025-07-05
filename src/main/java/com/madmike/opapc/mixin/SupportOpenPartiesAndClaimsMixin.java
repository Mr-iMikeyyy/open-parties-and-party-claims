package com.madmike.opapc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;
import xaero.map.gui.MapTileSelection;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.mods.pac.SupportOpenPartiesAndClaims;

import java.util.ArrayList;

@Mixin(SupportOpenPartiesAndClaims.class)
public class SupportOpenPartiesAndClaimsMixin {
    /**
     * Cancel the addRightClickOptions method to disable claim menu.
     */
    @Inject(
            method = "addRightClickOptions",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void opapc$cancelClaimRightClickMenu(
            GuiMap screen,
            ArrayList<RightClickOption> options,
            MapTileSelection mapTileSelection,
            MapProcessor mapProcessor,
            CallbackInfo ci
    ) {
        // Just return, do not build claim menu options
        ci.cancel();
    }
}
