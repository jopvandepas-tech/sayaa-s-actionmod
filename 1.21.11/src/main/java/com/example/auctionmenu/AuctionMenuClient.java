package com.example.auctionmenu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class AuctionMenuClient implements ClientModInitializer {
    private static KeyBinding openKey;
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("auctionmenu", "main"));

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.auctionmenu.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) client.setScreen(new AuctionScreen());
            }
            AuctionState.tick();
        });

        // Payment messages arrive as system messages. Player chat is ignored on purpose (can be faked).
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) AuctionState.onMessage(message.getString());
        });

        HudRenderCallback.EVENT.register(AuctionMenuClient::renderHud);
    }

    private static void renderHud(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tc) {
        boolean showEnded = !AuctionState.active && AuctionState.endedAt > 0
                && System.currentTimeMillis() - AuctionState.endedAt < 60_000;
        if (!AuctionState.active && !showEnded) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int x = 8, y = 8;
        ctx.fill(x - 4, y - 4, x + 190, y + 46, 0x99000000);
        ctx.drawItem(AuctionState.stack, x, y + 12);

        String title = AuctionState.stack.getName().getString();
        ctx.drawTextWithShadow(mc.textRenderer, title, x + 24, y, 0xFFFFFFFF);

        if (AuctionState.active) {
            long left = Math.max(0, AuctionState.endTime - System.currentTimeMillis()) / 1000;
            ctx.drawTextWithShadow(mc.textRenderer,
                    String.format("Time: %d:%02d", left / 60, left % 60), x + 24, y + 12, 0xFFFFFF55);
        } else {
            ctx.drawTextWithShadow(mc.textRenderer, "ENDED", x + 24, y + 12, 0xFFFF5555);
        }

        String bid = AuctionState.topBidder == null
                ? "No bids yet" + (AuctionState.minBid > 0 ? " (min " + AuctionState.fmt(AuctionState.minBid) + ")" : "")
                : AuctionState.fmt(AuctionState.topBid) + " - " + AuctionState.topBidder;
        ctx.drawTextWithShadow(mc.textRenderer, bid, x + 24, y + 24, 0xFF55FF55);
    }
}
