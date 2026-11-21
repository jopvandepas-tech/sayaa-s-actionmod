# Auction Menu (Fabric, client-side)

Press **K**, pick a block, time and minimum bid, click Start. Players bid with `/pay yourname amount`;
the highest bid, bidder and countdown show on your screen.

Versions: `26.3/` (Minecraft 26.3, Java 25) and `1.21.11/` (Minecraft 1.21.11, Java 21).

## Get the jar (no setup on your PC)
1. Make a free GitHub repo and upload everything in this folder (keep `.github/`).
2. Open the repo's **Actions** tab -> the "Build mod jars" run -> download the artifact for your version.
3. Unzip it and put the `.jar` in `.minecraft/mods` together with **Fabric API**.

## First-time setup in game
Open the menu (K) and check the **payment message regex**. If a payment doesn't register, a gray
`[Auction] Unmatched payment message: ...` line appears in chat. Adjust the regex to fit it
(group 1 = player name, group 2 = amount).
