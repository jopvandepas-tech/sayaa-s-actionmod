package com.example.auctionmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class AuctionScreen extends Screen {
    private static final int[] MINUTES = {1, 2, 5, 10, 30, 60};

    private static int timeIndex = 2;
    private static String minBidText = "0";

    private int selectedSlot = -1;
    private int gridX, gridY;

    public AuctionScreen() {
        super(Component.literal("Auction Menu"));
    }

    @Override
    protected void init() {
        gridX = width / 2 - 81;
        gridY = 36;

        EditBox minBidField = new EditBox(font, width / 2 - 81, 128, 80, 18, Component.literal("Min bid"));
        minBidField.setValue(minBidText);
        minBidField.setResponder(s -> minBidText = s);
        addRenderableWidget(minBidField);

        addRenderableWidget(Button.builder(Component.literal("Time: " + MINUTES[timeIndex] + " min"), b -> {
            timeIndex = (timeIndex + 1) % MINUTES.length;
            b.setMessage(Component.literal("Time: " + MINUTES[timeIndex] + " min"));
        }).bounds(width / 2 + 5, 127, 76, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Announce in chat: " + (AuctionState.announce ? "ON" : "OFF")), b -> {
            AuctionState.announce = !AuctionState.announce;
            b.setMessage(Component.literal("Announce in chat: " + (AuctionState.announce ? "ON" : "OFF")));
        }).bounds(width / 2 - 81, 150, 162, 20).build());

        EditBox regexField = new EditBox(font, width / 2 - 81, 187, 162, 18, Component.literal("Regex"));
        regexField.setMaxLength(300);
        regexField.setValue(AuctionState.regexText);
        regexField.setResponder(AuctionState::setRegex);
        addRenderableWidget(regexField);

        addRenderableWidget(Button.builder(Component.literal("Start Auction"), b -> start())
                .bounds(width / 2 - 81, 212, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Stop / Clear"), b -> {
            AuctionState.cancel();
            onClose();
        }).bounds(width / 2 + 1, 212, 80, 20).build());
    }

    private void start() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || selectedSlot < 0) return;
        ItemStack chosen = mc.player.getInventory().getItem(selectedSlot);

        // Hotbar blocks get selected in hand; blocks deeper in the inventory are just shown on the overlay.
        if (selectedSlot < 9) mc.player.getInventory().setSelectedSlot(selectedSlot);

        long min = Math.max(0, AuctionState.parse(minBidText));
        AuctionState.start(chosen, MINUTES[timeIndex] * 60_000L, min);
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        Minecraft mc = Minecraft.getInstance();

        String t = title.getString();
        g.text(font, t, width / 2 - font.width(t) / 2, 18, 0xFFFFFFFF, true);

        for (int i = 0; i < 36; i++) {
            int[] p = slotPos(i);
            if (i == selectedSlot) g.fill(p[0] - 1, p[1] - 1, p[0] + 17, p[1] + 17, 0xFF55FF55);
            else g.fill(p[0], p[1], p[0] + 16, p[1] + 16, 0x66000000);
            ItemStack s = mc.player.getInventory().getItem(i);
            if (isBlock(s)) g.item(s, p[0], p[1]);
        }
        g.text(font, "Min bid (supports 5k, 2m)", width / 2 - 81, 117, 0xFFAAAAAA, true);
        g.text(font, "Payment message regex (name, amount)", width / 2 - 81, 176, 0xFFAAAAAA, true);
    }

    private int[] slotPos(int i) {
        int col = i % 9;
        int row = i < 9 ? 3 : (i - 9) / 9;
        return new int[]{gridX + col * 18, gridY + row * 18 + (i < 9 ? 4 : 0)};
    }

    private boolean isBlock(ItemStack s) {
        return !s.isEmpty() && s.getItem() instanceof BlockItem;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Minecraft mc = Minecraft.getInstance();
        double mx = event.x(), my = event.y();
        for (int i = 0; i < 36; i++) {
            int[] p = slotPos(i);
            if (mx >= p[0] && mx < p[0] + 16 && my >= p[1] && my < p[1] + 16
                    && isBlock(mc.player.getInventory().getItem(i))) {
                selectedSlot = i;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
