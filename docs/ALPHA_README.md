# Kingdom SMP — 0.12.0-alpha.1 (isolated early-game alpha)

This is an **experimental Tier 1–3 alpha** built from `rebuild/royal-gilded` at commit `5acf1f7489511fd05854f17d99c717e6305856ed`. It is not Kingdom SMP 1.0.0 and does **not** certify the full 277-mod pack.

## Requirements

- Minecraft **Java 1.21.1**, **NeoForge 21.1.265**, **Java 21** on client and server.
- Install the exact same Kingdom SMP alpha mod JAR on the server and every participating client.
- Third-party mod compatibility and minimum required dependencies must be resolved against the real modpack; the repository has documented a *subset* of mods and versions in `docs/VERIFIED_INTEGRATION_ARTIFACTS.md`. The 277-entry reference is in `docs/sources/MOD_SNAPSHOT_2026-10-31.txt`.
- Always use a new test world or a complete copied backup of an existing world. **Never downgrade a world used with this alpha to an earlier checkpoint.** Backup the complete world, configs, and player data before each upgrade.

## Intended playable scope

- Fresh-world starting Builder's Hut, Aldric, real contributions, Tier 2 Town Hall and Tier 3 Bank.
- Royal Architect purchases and first-buyer building plans, progressive protected construction.
- Coins, banking, dynamic NPC shops, auctions/buy orders, private mail and available contracts.
- Profession selection (maximum three active) and currently implemented, validated quest routes. Farmer's 24 objectives are implemented; the other professions are **partially implemented**, not complete.
- Bounty Hall and Fisherman's Wharf services in their verified limited scope.
- Royal Gilded menus and early settlement FTB guide (FTB optional; full FTB campaign not included).

### Not included as completed gameplay

- Naturally reachable Tier 4–12 progression, full mod/recipe unlock graph and world borders.
- All 288 playable profession quests, 12 fully accepted professions, most Master signature items.
- Full FTB campaign, all admin recovery controls, Royal Legacy finale, complete modpack validation.
- Optional/modded services must fail closed if a required mod/version is absent.

## Installation

1. Install Minecraft Java **1.21.1**, NeoForge **21.1.265**, and Java **21**.
2. Put `mods/kingdom-smp-core-0.12.0-alpha.1.jar` in the dedicated server's `mods/` folder and every client's `mods/` folder. Remove older Kingdom SMP JARs first.
3. Install the intended modpack and configure matching NeoForge/required dependencies. Review `docs/VERIFIED_INTEGRATION_ARTIFACTS.md` and `docs/PUBLIC_NPC_CLAIM_SETUP.md` in the source repository before configuring claims/NPC public service.
4. Start a **fresh test world** and accept Minecraft's EULA manually on the server. Do not disable FTB Chunks or OPAC globally to force access.
5. Validate the free Hut plan, real Tier 1 donations, Town Hall, Tier 2 resources/road, Bank, currency and mailbox before inviting friends.
6. Check `KNOWN_LIMITATIONS.txt` and the CI evidence before treating this build as tested.

## Safe operation and validation

The automation packages the JAR only when the Java build succeeds, and separately records whether a native GameTest dedicated-server smoke run passes. A packaged JAR alone is **not proof of complete runtime or 277-mod compatibility**. The version's test status must be checked in the exact GitHub Actions run.

For administrators, backup worlds before experiments, inspect permission failures rather than granting everyone OP, and test NPC public-use settings on a fresh server copy. See `docs/ADMIN_COMMAND_REFERENCE.md` and `docs/PUBLIC_NPC_CLAIM_SETUP.md`.

## Source and provenance

Development is isolated on `alpha/0.12.0-alpha.1`. Original branch `rebuild/royal-gilded` and `main` remain unchanged. Automated artifacts include the source SHA and SHA-256 sums. No final 1.0.0 claim.
