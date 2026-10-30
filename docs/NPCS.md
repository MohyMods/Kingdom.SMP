# Kingdom NPC specification

Source: S3. Initial NPCs support the Builder’s Hut → Town Hall slice. Stable identity, building/home association, persistent lifecycle and server-authoritative interaction are required. Names are presentation, not identity keys.

## Aldric Stonehand — Master Builder

Default name: **Aldric Stonehand**, configurable without replacing his stable identity. Home/work association: the admin-established furnished timber-and-stone Builder’s Hut, available before the Town Hall.

Personality: experienced, hardworking, slightly grumpy, proud and passionate about building. Dialogue must convey identity and role, with meaningful choices for showing blueprints, constructing, asking what he is working on, asking how long work will take, and asking about him. Do not invent a promised completion time before the scheduler/rate policy exists; show actual current status and any pause/failure reason.

Support a coherent routine and safe work behavior. Progress is authoritative server state, not a dialogue animation. Blocked/unloaded sites or failed paths must pause/report safely. Do not silently force-load chunks or destroy obstacles to make the NPC appear productive.

## Elara Vale — Royal Steward

Default name and role: **Elara Vale**, Royal Steward. She arrives only after the Town Hall construction passes required-block verification and completion is durably recorded. Associate her persistent identity/home with the kingdom-owned hall; reconnect or restart must not spawn duplicate stewards.

Supplied introduction example: “Greetings! I’m Elara Vale, your new Royal Steward. This hall is where our kingdom’s future will be decided.” This is dialogue guidance, not authority for extra services. Her Town Hall interactions expose only approved and actually implemented capabilities.

## Dialogue and lifecycle requirements

Royal Gilded dialogue shows NPC name/role, readable text, meaningful choices, back/close behavior and clear unavailable reasons. Server verifies current player, NPC identity, proximity/access and action eligibility for mutation. Clients cannot turn a dialogue choice into a trusted entitlement or complete a construction.

Persist NPC identity, display configuration, building/home association, lifecycle and current work/assignment as appropriate. Verify entity/building associations on load. NPC death, despawn, building destruction, home obstruction and administrative recovery semantics remain open; avoid silently resurrecting/replacing entities in a way that can duplicate inventory/rewards or bypass approvals.

## Expansion boundaries

Keep clean extension boundaries for future NPC roles, routines, relationship/reputation systems and event reactions. Capability architecture does **not** authorize implementing those features now. In particular, do not restore the excluded Weekly Events feature or other removed custom scope through an NPC submenu.

Required checks: name changes preserve identity, server restart association, one NPC per registered role instance, invalid/out-of-range interaction rejected, blocked path safely reported, unloaded work paused, steward arrival exactly once after verified completion, and damage/death recovery once its policy is approved. These checks are not claimed executed here.
