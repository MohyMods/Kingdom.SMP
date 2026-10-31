# Kingdom SMP Core

> **Current authority — 2026-10-30 progression correction:** Town Hall and its blueprint are Tier 12 only. Tier 1 completion advances to Tier 2 and recognizes the highest contributor; its approved building reward is the Founders’ Lodge. No early Town Hall, Elara, Bank or Royal Trading dependency may be implemented from historical descriptions below. Earlier bank/trading locations and exact gates require separate approval; the Charter remains available after Tier 2 independently of the Town Hall. The existing hut, Aldric and Royal Gilded design are approved.

Status: fresh rebuild authorized; foundation and Royal Gilded vertical slice in development. Last updated: 2026-10-30 (Asia/Riyadh).

## Vision

A medieval-fantasy Minecraft multiplayer experience for approximately ten friends, with room to expand. Players establish a shared kingdom and advance it through twelve tiers toward technological and magical civilization. Exploration, cooperation, specialization, player markets, and shared town infrastructure should remain useful throughout the season. Automation and powerful equipment must be earned.

Production quality means secure and recoverable multiplayer behavior, maintainable modules, intentional balance, and polished interfaces; compilation alone does not establish readiness.

## Confirmed platform

| Concern | Requirement |
| --- | --- |
| Project | Kingdom SMP Core |
| Minecraft | Java Edition 1.21.1 |
| Loader | NeoForge |
| Java | 21 |
| Build | Gradle |
| Version control | Git |
| Architecture | Modular, maintainable, server-authoritative |

Current verified toolchain: NeoForge 21.1.265, Gradle 9.2.1 and Java 21. Third-party mod artifacts remain unverified. No compatibility claims or API signatures are approved by these documents.

## Sources and authority

S1: the user's initial project brief in this conversation, received 2026-10-30.

S2: the user's “KINGDOM SMP — COMPLETE PROJECT REBUILD” instruction, received 2026-10-30. S2 authorizes a fresh implementation, a dedicated development branch, build/test work, incremental commits, and Royal Gilded custom interfaces. It supersedes S1's planning-only restriction. Exact gameplay rules still require authoritative specifications; recommendations are not silently promoted to gameplay decisions.

S3: the subsequent Tier 1, Builder’s Hut, NPC, construction, and Town Hall specification, received 2026-10-30. It supplies the authoritative first progression slice and changes immediate dependency order: foundation → Builder’s Hut → contributions/reward → construction → completed Town Hall → approved expansion. See [kingdom progression](KINGDOM_PROGRESSION.md), [construction](CONSTRUCTION.md), and [NPCs](NPCS.md).

The user reports extensive prior planning and older removed designs. Those original documents are unavailable; none have been read. Actual source documents and later approved decisions take precedence when supplied. Conflicts must be recorded and reviewed before changing requirements.

Statuses used throughout:

- **Confirmed:** explicitly specified in S1, S2, S3, or subsequently approved by the user.
- **Proposed:** recommendation requiring review; not authorization to implement.
- **Open:** missing or conflicting specification.
- **Excluded:** explicitly removed from custom mod scope; restoration requires approval.

## Current authority and inventory

S2 authorizes creating a fresh NeoForge codebase, configuring tools, implementing systems and client interfaces, running builds/tests, fixing new code, updating docs, and making development commits on a dedicated branch. Do not push main or merge without approval. Design and review layout/interaction flow before implementing major screens; report visual/runtime checks honestly. Independent ordinary engineering decisions are authorized; material unresolved gameplay or technical-direction decisions still require input.

The rebuild began from a blank `Home` file and staged planning documents; there was no previous implementation in the available checkout. Preserve these source references and all uncommitted work. See [repository audit](REPOSITORY_AUDIT.md) for historical evidence and the [development log](DEVELOPMENT_LOG.md) for actual implementation/validation results. No unavailable previous codebase is being repaired or recreated.

## Documentation map

- [Requirements and acceptance constraints](REQUIREMENTS.md)
- [Kingdom progression](KINGDOM_PROGRESSION.md)
- [Blueprints and construction](CONSTRUCTION.md)
- [NPCs and building services](NPCS.md)
- [Professions](PROFESSIONS.md)
- [Economy](ECONOMY.md)
- [Technology balance](TECHNOLOGY_BALANCE.md)
- [UI/UX and first three interface drafts](UI_UX_SPEC.md)
- [Architecture proposal](ARCHITECTURE.md)
- [Milestones and dependencies](DEVELOPMENT_ROADMAP.md)
- [Decisions, contradictions, and questions](DECISIONS.md)
- [Development log](DEVELOPMENT_LOG.md)
