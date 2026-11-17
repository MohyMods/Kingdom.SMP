# Durable economy history

The canonical economy journal supports schema1 worlds without rewriting them on read. Its first successful rotation publishes schema2: an ordered manifest of immutable closed segments plus recent active records. Balances, original journal identity, transaction IDs, revision, escrow, entitlements, tax totals and player inventory receipts retain their original meaning. This is maintenance of committed history, not a reset or a recovery grant.

Automatic rotation starts at8,192 active records or8MiB and retains128 recent records. Each closed segment contains at most4,096 records and4MiB uncompressed NBT. The manifest allows256 segments and the complete journal allows1,000,000 audited operations, including reserved future recovery work. Active admission remains bounded at50,000 operations and12MiB. These are finite safeguards, not an unlimited-history or full-modpack performance guarantee. Admission preserves space reserved for existing escrow and delivery recovery.

## Administration

Permission4 is required for all three commands:

- `/kingdom admin economy archive inspect` reads actual capacity and pending inventory state.
- `/kingdom admin economy archive review <reason>` reviews the exact current state and sends a clickable Minecraft chat confirmation.
- `/kingdom admin economy archive confirm <nonce>` consumes that actor-bound review within60seconds. A changed journal, expired/replayed review, different actor or pending native inventory operation refuses maintenance.

The successful publication logs economy identity, administrator UUID, reason, closed sequence, segment count and unchanged revision. Automatic rotation records the console/system identity. It never deletes history or modifies player inventory. Reconcile an interrupted inventory operation through its matching existing recovery protocol before maintenance; never clear a pending record manually.

## Backup and failure handling

Stop cleanly before making a consistent backup of the complete world. Include `data/kingdomsmp_economy.dat`, `data/kingdomsmp_economy.created`, the entire `data/kingdomsmp_economy_archive/` directory, all matching player saves and other kingdom journals. Restoring only a purse or player file is unsafe. Earlier binaries cannot read schema2; rollback requires the complete matching pre-migration backup.

Segments bind the original identity, global sequence range, previous segment hash and administrator audit. Their compressed file SHA256 and canonical filename must match the root manifest. Missing, modified, foreign, reordered, duplicate-ID or disconnected history fails closed. Missing both root authority files while archive files remain also refuses financial genesis. Preserve the files and logs, stop financial activity and restore or investigate a matching complete backup. Do not rename, edit or delete segments to force startup.

Publication requires same-directory atomic file replacement, hard-link creation and durable file writes. Unix directory metadata is forced; Windows retains the existing process-interruption durability contract. Unsupported filesystems fail closed. Unreferenced files left by interrupted publication are ignored, never replayed and never grant money. Keep them for investigation; no automatic garbage collector is implemented.

## Executed scope

Full Gradle build:398JUnit tests, zero failures/errors (`work/production-economy-archive-complete-build.log`). Disk tests cover schema1 preservation, exact financial/native receipt replay, failures before/after root replacement, tampering, pending refusal, automatic rotation and50,001 global operations with old IDs preserved.

Three actual dedicated JVMs passed `work/production-economy-archive-public-counter-native.log`: earned Farmer24/ranks/both tools, funded auctions/bids/orders, actual reviewed maintenance and rejection cases; restart; partial fills/refunds/settlement/private mail and exact6coin tax; second restart preserving receipts, professions, construction and NPCs. Fixture supplies, growth, AI and deadlines are explicitly accelerated. Independent FTB/OPAC review-revocation and Wharf restart regression also passed (`work/production-archive-wharf-revocation-regression.log`).

Actual ordinary-client verification used the existing private Wharf world: clickable administrator archive closed26 records without changing2,587coins or revision26; de-opped Survival customer bought one1,500coin rod and claimed exact private mail, ending at1,087coins/revision28 with the original damaged rod and four perch retained. Actual client and dedicated-server restart retained the same1,087coins/revision28, exact inventory, closed segment and original construction hash/NPCs. Captures and proof remain in ignored `work/archive-client-*`. Full277-modpack acceptance, million-record load testing and natural-duration balance remain open.
