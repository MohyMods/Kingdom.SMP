# Builder's Hut dialogue — F2 layout and interaction contract

Defined before implementation on 2026-10-30. This extends the approved Royal Gilded dialogue direction without adding progression or services.

The screen opens from a nearby, registered Aldric interaction authorized by the server. Its header identifies the current configurable NPC name and Master Builder role. A framed live villager portrait sits beside that identity. Charcoal stone surrounds restrained brass rules, selected gold controls, and a parchment reading panel. Vanilla typography remains at normal scale.

Four choices are real server-validated inspections: Blueprints, Current work, Build time, and About Aldric. Each sends the NPC UUID, bounded topic, and positive correlation ID; the server rechecks identity and proximity. A pending request disables repeated choices. Only its matching response updates the active conversation; navigation, disconnect, timeout, or an out-of-range NPC cancels it. Positive responses cannot reopen a closed screen. The initial server-initiated interaction response has ID zero and may open only with no other screen and the same living NPC locally within reach; its unsolicited delivery retains a small initial-packet timing limitation.

At 320 × 240 GUI pixels, choices use two rows, the portrait stays compact, and the reading panel scrolls. Wider windows place the four choices on one row and expand the reading panel. Page Up/Down and mouse wheel scroll the text. Back returns to the greeting through the same validated request path; Leave and Escape close immediately. All text remains clipped to the panel.

The catalogue currently contains one inspected Town Hall reward entry. It explains the highest-contributor rule, four exact First Hearth targets, included construction materials, and exclusive one-use entitlement. It explicitly reports that placement and construction are not available in this checkpoint. There is no inactive Build or Preview button masquerading as a connected feature. The current-work response shows actual server hut/NPC status, and the build-time response gives no invented schedule. No dialogue choice consumes items, elects winners, grants rewards, changes tiers, or unlocks Town Hall services.

Validation requires actual compilation, physical-client initialization, visual inspection where graphics are available, and request/navigation/disconnect checks. This design document does not establish any test result.
