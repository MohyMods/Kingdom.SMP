# Kingdom SMP Core

Status: planning and architecture only. Last updated: 2026-10-30 (Asia/Riyadh).

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

Exact NeoForge, Gradle, mappings, and third-party artifact versions are **not selected or verified**. No compatibility claims or API signatures are approved by these documents.

## Sources and authority

S1: the user's project brief in this conversation, received 2026-10-30. All confirmed requirements currently derive from S1. This documentation records that baseline permanently but does not promote recommendations into approved requirements.

The user reports extensive prior planning and older removed designs. Those original documents are unavailable; none have been read. Actual source documents and later approved decisions take precedence when supplied. Conflicts must be recorded and reviewed before changing requirements.

Statuses used throughout:

- **Confirmed:** explicitly specified in S1 or subsequently approved by the user.
- **Proposed:** recommendation requiring review; not authorization to implement.
- **Open:** missing or conflicting specification.
- **Excluded:** explicitly removed from custom mod scope; restoration requires approval.

## Current authority and inventory

Authorized: inspect files, analyze supplied material, write documentation, design interface drafts, and propose architecture and plans. Gameplay implementation and starting the mod build are **not authorized**. Interface implementation additionally requires layout and interaction review.

The checkout has only a blank `Home` file. No mod, Gradle project, tests, dependencies, assets, CI, or working gameplay systems exist. See [repository audit](REPOSITORY_AUDIT.md). No project tooling was installed for this planning phase.

## Documentation map

- [Requirements and acceptance constraints](REQUIREMENTS.md)
- [Kingdom progression](KINGDOM_PROGRESSION.md)
- [Professions](PROFESSIONS.md)
- [Economy](ECONOMY.md)
- [Technology balance](TECHNOLOGY_BALANCE.md)
- [UI/UX and first three interface drafts](UI_UX_SPEC.md)
- [Architecture proposal](ARCHITECTURE.md)
- [Milestones and dependencies](DEVELOPMENT_ROADMAP.md)
- [Decisions, contradictions, and questions](DECISIONS.md)
- [Development log](DEVELOPMENT_LOG.md)
