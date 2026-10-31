# Technology and balance

> **D-022 — planning only, current dimension authority:** End unlocks at **Tier 12**, superseding every End-at-9 design. Target schedule: Aether 4; Nether 5; Twilight Forest 6; Otherside 8; reviewed Ad Astra stages 10–11; End 12. No mandatory prerequisite for reaching 12 may depend on End resources, including indirect recipes. Dragon/End exploration belongs after the unlock; **Royal Legacy Monument** remains the proposed final construction reward. Existing code/data/worlds are unchanged. See [dimension schedule, dependency graph and open recipe audit](DIMENSION_PROGRESSION.md). Implementation remains on hold.

Confirmed design includes Create, Mekanism, Occultism, Mystical Agriculture, Productive Bees, Ars Nouveau, Iron’s Spells, and other separately approved compatible systems. No exact mod versions, artifacts, optional dependencies, licenses, API behavior, or Minecraft 1.21.1 NeoForge compatibility have been verified. Do not interpret this list as a validated installable modpack.

Technology must unlock progressively by kingdom tier and appropriate professions. D-022 target schedule is documented in DIMENSION_PROGRESSION.md: End at Tier 12, Nether remains Tier 5; Creative Energy Cube requires Tier 12 and active Master Mechanic. Offline automation being disabled is the current **proposal**, pending an enforceable definition.

## Proposed integration workflow

Before adopting any dependency, record official artifact source, exact release, loader and Minecraft compatibility, transitive requirements, license/distribution constraints, API documentation/source evidence, and observed dedicated-server plus client results. Pin artifacts and preserve verification. Do not infer compatibility from similar mod names or another Minecraft release. Keep direct mod imports within version-specific integration adapters; define required versus optional installation explicitly.

Verify available hooks against the selected NeoForge/mod sources before designing enforcement code. No method names or event coverage are claimed in this plan. If a necessary operation has no supported hook, report the gap and compare a narrower rule or disabling that content against a documented, tested patch; never silently leave an exploit path open.

## Gate coverage matrix to complete

For each gated content ID, define separate rules for acquiring, crafting, processing, placing, using, transferring, and operating via automation. Enumerate recipes, machine recipes, loot, structures, trades, drops, quest rewards, bee/crop output, summons, recycling, cross-mod conversions, inventory import, and administrative grants. Also evaluate dispensers, fake players, hoppers/pipes, storage networks, existing placed blocks, and cached machine work after a gate changes.

Classify whether a rule is global (kingdom tier) or personal (profession/rank). Machine ownership, group access, and delegation must be explicit: many automated operations have no meaningful online player actor. Default-deny missing personal context is a proposed security default, not an approved balance rule.

## Offline automation: unresolved definition

Could mean stopping when zero server players are online, when an owner is offline, or when no authorized profession holder is online. Also distinguish unloaded chunks, force-loaded chunks, passive growth, elapsed-time catch-up, and machines continuing work across restarts. Owner-offline enforcement requires reliable ownership attribution across mods and shared town machinery; disabling chunk loaders alone does not satisfy every interpretation.

Recommend starting with a policy that can be demonstrated using selected mods' supported configuration, then testing it. Do not promise a universal switch. Resolve scope before enabling progression-sensitive production.

## Balance evidence needed

Tier costs and approximate playtime targets; currency sources/sinks; resource output rates; progression bypass graph; equipment power bands; allowed automation/flight/teleportation/storage tools; shared infrastructure ownership; intended season duration. Profile representative machines with ten clients and simulated workloads. Agree on performance targets before claiming adequate capacity; no TPS/MSPT budget or hardware spec has been approved.

## D-022 integration audit status

Aether, Twilight Forest, Deeper and Darker / Otherside and Ad Astra are now scheduled dimension design requirements, not verified compatible dependencies. Current repository/build/mods inspection supplies no pinned final pack or installed recipe data for them. Audit their exact NeoForge 1.21.1 compatibility, prerequisites, entry/exit/alternate teleport paths and every Ad Astra rocket/fuel/oxygen/machine/planetary-material chain before approving required progression. Unknown indirect End dependencies keep pre-12 modded ingredient slots optional and unapproved. See DIMENSION_PROGRESSION.md for the per-system audit and safe-world rollout decisions. No integration or recipe was installed/changed for this planning task.
