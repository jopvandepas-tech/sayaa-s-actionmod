package com.example.auctionmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AuctionState {
    // Group 1 = player name, group 2 = amount. Edit this in the menu to match DonutSMP's message.
    public static final String DEFAULT_REGEX = "(?i)(\\w{3,16}) (?:has )?(?:paid|sent) you \\$?([\\d.,]+\\s?[kmb]?)";

    public static boolean active = false;
    public static long endedAt = 0;
    public static ItemStack stack = ItemStack.EMPTY;
    public static long endTime = 0;
    public static long minBid = 0;
    public static long topBid = 0;
    public static String topBidder = null;
    public static boolean announce = true;
    public static String regexText = DEFAULT_REGEX;
    public static Pattern pattern = Pattern.compile(DEFAULT_REGEX);

    public static void setRegex(String s) {
        try {
            pattern = Pattern.compile(s);
            regexText = s;
        } catch (PatternSyntaxException ignored) {
        }
    }

    public static void start(ItemStack s, long durationMs, long min) {
        stack = s.copyWithCount(1);
        endTime = System.currentTimeMillis() + durationMs;
        minBid = min;
        topBid = 0;
        topBidder = null;
        endedAt = 0;
        active = true;
        say("/pay <you> <amount> to bid on " + stack.getHoverName().getString()
                + " | min " + fmt(min) + " | " + (durationMs / 60000) + " min");
    }

    public static void stop() {
        if (!active) return;
        active = false;
        endedAt = System.currentTimeMillis();
        String result = topBidder == null
                ? "Auction ended with no bids."
                : "Auction ended! Winner: " + topBidder + " with " + fmt(topBid);
        local(result);
        say(result);
    }

    public static void cancel() {
        active = false;
        endedAt = 0;
    }

    public static void tick() {
        if (active && System.currentTimeMillis() >= endTime) stop();
    }

    /** Only called for system/game messages, never player chat, so players can't fake payments. */
    public static void onMessage(String text) {
        if (!active) return;
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            String name = m.group(1);
            long amount = parse(m.group(2));
            if (amount <= 0) return;
            if (amount >= minBid && amount > topBid) {
                topBid = amount;
                topBidder = name;
                local("New top bid: " + name + " - " + fmt(amount));
                say("New top bid: " + name + " with " + fmt(amount) + "!");
            }
        } else if (text.toLowerCase(Locale.ROOT).contains("paid")) {
            local("Unmatched payment message (fix the regex in the menu): " + text);
        }
    }

    public static long parse(String raw) {
        try {
            String s = raw.trim().toLowerCase(Locale.ROOT).replace(",", "").replace("$", "").replace(" ", "");
            double mult = 1;
            if (s.endsWith("k")) mult = 1_000;
            else if (s.endsWith("m")) mult = 1_000_000;
            else if (s.endsWith("b")) mult = 1_000_000_000;
            if (mult != 1) s = s.substring(0, s.length() - 1);
            return (long) (Double.parseDouble(s) * mult);
        } catch (Exception e) {
            return -1;
        }
    }

    public static String fmt(long n) {
        return "$" + String.format(Locale.US, "%,d", n);
    }

    private static void local(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal("[Auction] " + msg));
    }

    private static void say(String msg) {
        if (!announce) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String me = mc.player.getName().getString();
        mc.player.connection.sendChat("[Auction] " + msg.replace("<you>", me));
    }
}
