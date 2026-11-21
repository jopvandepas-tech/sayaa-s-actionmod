package com.example.auctionmenu;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class AuctionMenuClient implements ClientModInitializer {
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("auctionmenu", "main"));
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.auctionmenu.open", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    client.gui.setScreen(new AuctionScreen());
                }
            }
            AuctionState.tick();
        });

        // Payment messages arrive as system messages. Player chat is ignored on purpose (can be faked).
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) AuctionState.onMessage(message.getString());
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("auctionmenu", "hud"), AuctionMenuClient::renderHud);
    }

    private static void renderHud(GuiGraphicsExtractor g, DeltaTracker tracker) {
        boolean showEnded = !AuctionState.active && AuctionState.endedAt > 0
                && System.currentTimeMillis() - AuctionState.endedAt < 60_000;
        if (!AuctionState.active && !showEnded) return;

        Minecraft mc = Minecraft.getInstance();
        int x = 8, y = 8;
        g.fill(x - 4, y - 4, x + 190, y + 46, 0x99000000);
        g.item(AuctionState.stack, x, y + 12);

        g.text(mc.font, AuctionState.stack.getHoverName().getString(), x + 24, y, 0xFFFFFFFF, true);

        if (AuctionState.active) {
            long left = Math.max(0, AuctionState.endTime - System.currentTimeMillis()) / 1000;
            g.text(mc.font, String.format("Time: %d:%02d", left / 60, left % 60), x + 24, y + 12, 0xFFFFFF55, true);
        } else {
            g.text(mc.font, "ENDED", x + 24, y + 12, 0xFFFF5555, true);
        }

        String bid = AuctionState.topBidder == null
                ? "No bids yet" + (AuctionState.minBid > 0 ? " (min " + AuctionState.fmt(AuctionState.minBid) + ")" : "")
                : AuctionState.fmt(AuctionState.topBid) + " - " + AuctionState.topBidder;
        g.text(mc.font, bid, x + 24, y + 24, 0xFF55FF55, true);
    }
}
