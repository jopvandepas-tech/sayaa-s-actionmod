package com.example.auctionmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class AuctionScreen extends Screen {
    private static final int[] MINUTES = {1, 2, 5, 10, 30, 60};

    private static int timeIndex = 2;
    private static String minBidText = "0";

    private int selectedSlot = -1;
    private int gridX, gridY;
    private TextFieldWidget minBidField, regexField;

    public AuctionScreen() {
        super(Text.literal("Auction Menu"));
    }

    @Override
    protected void init() {
        gridX = width / 2 - 81;
        gridY = 36;

        minBidField = new TextFieldWidget(textRenderer, width / 2 - 81, 128, 80, 18, Text.literal("Min bid"));
        minBidField.setText(minBidText);
        minBidField.setChangedListener(s -> minBidText = s);
        addDrawableChild(minBidField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Time: " + MINUTES[timeIndex] + " min"), b -> {
            timeIndex = (timeIndex + 1) % MINUTES.length;
            b.setMessage(Text.literal("Time: " + MINUTES[timeIndex] + " min"));
        }).dimensions(width / 2 + 5, 127, 76, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Announce in chat: " + (AuctionState.announce ? "ON" : "OFF")), b -> {
            AuctionState.announce = !AuctionState.announce;
            b.setMessage(Text.literal("Announce in chat: " + (AuctionState.announce ? "ON" : "OFF")));
        }).dimensions(width / 2 - 81, 150, 162, 20).build());

        regexField = new TextFieldWidget(textRenderer, width / 2 - 81, 187, 162, 18, Text.literal("Regex"));
        regexField.setMaxLength(300);
        regexField.setText(AuctionState.regexText);
        regexField.setChangedListener(AuctionState::setRegex);
        addDrawableChild(regexField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Start Auction"), b -> start())
                .dimensions(width / 2 - 81, 212, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Stop / Clear"), b -> {
            AuctionState.cancel();
            close();
        }).dimensions(width / 2 + 1, 212, 80, 20).build());
    }

    private void start() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || selectedSlot < 0) return;
        ItemStack chosen = mc.player.getInventory().getStack(selectedSlot);

        // Put the block in your hand so people can see what's being sold
        if (selectedSlot >= 9) {
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, selectedSlot,
                    mc.player.getInventory().getSelectedSlot(), SlotActionType.SWAP, mc.player);
        } else {
            mc.player.getInventory().setSelectedSlot(selectedSlot);
        }

        long min = Math.max(0, AuctionState.parse(minBidText));
        AuctionState.start(chosen, MINUTES[timeIndex] * 60_000L, min);
        close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFFFFFFF);
        MinecraftClient mc = MinecraftClient.getInstance();
        for (int i = 0; i < 36; i++) {
            int[] p = slotPos(i);
            if (i == selectedSlot) ctx.fill(p[0] - 1, p[1] - 1, p[0] + 17, p[1] + 17, 0xFF55FF55);
            else ctx.fill(p[0], p[1], p[0] + 16, p[1] + 16, 0x66000000);
            ItemStack s = mc.player.getInventory().getStack(i);
            if (isBlock(s)) ctx.drawItem(s, p[0], p[1]);
        }
        ctx.drawTextWithShadow(textRenderer, "Min bid (supports 5k, 2m)", width / 2 - 81, 117, 0xFFAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "Payment message regex (name, amount)", width / 2 - 81, 176, 0xFFAAAAAA);
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
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        MinecraftClient mc = MinecraftClient.getInstance();
        for (int i = 0; i < 36; i++) {
            int[] p = slotPos(i);
            if (mx >= p[0] && mx < p[0] + 16 && my >= p[1] && my < p[1] + 16
                    && isBlock(mc.player.getInventory().getStack(i))) {
                selectedSlot = i;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }
}
