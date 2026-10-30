# UI/UX specification — review draft

All layouts and interactions below are **proposals awaiting review**, not approved UI or implemented capabilities. No textures or in-game screenshots exist. S1 requires reviewed mockups or structured layouts for every major screen before implementation.

## Presentation choice

| Approach | Capabilities and limits | Distribution and compatibility |
| --- | --- | --- |
| Standard inventory menus | Familiar slot grid, item icons, hovers, inventory behavior. Styling and typography constrained by existing container presentation; rich scrolling and complex panels require awkward pagination or extensions. Real container behavior creates shift-click/drag/number-key/creative-mode risks if decorative slots are implemented carelessly. | Vanilla container presentation alone need not require custom client UI code. Custom content and custom menu types may still require client installation; this does not make a NeoForge modpack vanilla-client compatible. |
| Custom client screens, with server-managed container behavior where needed | Textured panels, dedicated buttons, progress bars, scrolling, tooltips, responsive layouts, and explicit confirmations are feasible with Minecraft client rendering/input, subject to implementation and testing. Screen code must never decide balances or grants. | Requires the matching client mod, assets, and protocol compatibility. Client-only code must be isolated so a dedicated server can load. Must test GUI scale, resource packs, input focus, and mod UI conflicts. |

**Recommendation:** custom screens for the shared visual language, with authoritative server requests and suitable server-managed inventory interaction for coin transfers. A custom screen does not remove container synchronization or transaction requirements. The final widget/menu/network APIs must be verified against selected NeoForge 1.21.1 sources. No fictional UI framework is assumed.

## Proposed design language

Parchment surfaces, dark oak framing, restrained brass accents, deep ink text, and compact heraldic profession symbols. Avoid translucent text panels and tiny decorative fonts. Preserve readable Minecraft text; custom typography and asset licensing require review. Use a consistent title/breadcrumb header, left navigation rail, central task panel, and bottom back/help/status area. Back restores prior filters and selection; Escape closes transient help/confirmation first, then the screen.

Never encode state only by color:

| State | Proposed visible treatment |
| --- | --- |
| Locked | Lock icon + prerequisite text; inspectable, action disabled |
| Unlocked | Normal border + action label |
| Completed | Checkmark + explicit completed label |
| Selected | Brass outline + selected label/focus marker |
| Unavailable | Disabled action + precise reason (e.g. bank counter out of reach) |
| Pending | Busy label; suppress repeat submit while awaiting result |

Tooltips explain effect, requirements, price/tax/total where relevant, and why an action is disabled. Never require hovering for essential price or prerequisites. Progress shows numeric current/target and label as well as a bar. Render cached view data; use pagination and bounded updates rather than sending full auction inventories every tick. Keyboard focus order, scroll behavior, GUI scale, translated text expansion, and small windows need review and testing. Do not assume screen-reader support without validation.

Important mutations use a review step showing action, items/quantity, total debit, tax breakdown if applicable, resulting balance where appropriate, and consequences. Separate Confirm from Cancel, prevent accidental carryover clicks, and revalidate on server commit. A changed quote returns to review; no silent extra charge. Failure messages preserve context and state what the player can do next. Success displays the authoritative receipt summary without adding a player-facing Economy Ledger.

## First three interfaces to design

Design order: Kingdom Overview → Professions and Quests → Town Hall Bank. These establish global, personal, and transaction patterns before markets and Master Shops. Market/admin screens are deferred to separate reviewed designs.

### 1. Kingdom Overview

Structured custom-screen regions (not inventory slot indices):

| Region | Content | Interaction |
| --- | --- | --- |
| Header | Kingdom name placeholder; current tier; breadcrumb | Name source and editing permissions unresolved |
| Left rail | Kingdom, Professions, Bank, Market, Help | Navigation reflects authorization/location; Bank mutation still needs counter |
| Main upper | Twelve tier markers with current/completed/locked states | Select marker to inspect requirements, including locked tiers |
| Main center | Selected tier milestone list, contribution totals, progress | Details reveal credit and consumption rules once approved |
| Right detail | Unlocks and unmet prerequisites | No hidden unlock grants from clicking |
| Footer | Back, Help, server status | Advancement/donation buttons omitted until governance rules approved |

Opening is read-only by default. If contributions or advancement are approved, their confirmation screens need a separate layout. Missing milestone data must display an explicit configuration problem, not fabricated objectives or a false zero-progress state.

### 2. Professions and Quests

Symmetric 4-column × 3-row profession card grid, displayed left-to-right in the confirmed roster order. Each card has icon, name, rank, active/inactive label, and quest count. Header shows `Active: n/3`. Selection opens a detail panel with three rank tabs and eight quest rows per rank, with scrolling/pagination if necessary. Show prerequisites and preserved inactive progress clearly.

| Region | Content/behavior |
| --- | --- |
| Header/left rail | Shared shell; personal view identified |
| Grid | Twelve cards, consistent sizes and state indicators |
| Details | Selected profession, rank tabs, quest objectives and claim state |
| Actions | Inspect quest; request activation/switch; Master Shop entry only once catalogue/navigation reviewed |
| Footer | Back to prior selection; Help explains activation cap and inactive progress |

Switch review: old and new active sets, charge of 15,000 coins **only when the approved switch rule applies**, funding source, preserved progress, Confirm/Cancel. Do not preview invented quest names, rank thresholds, or rewards as final content. Reward claiming and Master Shop interactions require separately reviewed details.

### 3. Town Hall Bank

| Region | Content/behavior |
| --- | --- |
| Header | Town Hall Bank; authoritative counter access state |
| Balance summary | Virtual balance and eligible physical coins, clearly separated |
| Main tabs | Deposit / Withdraw |
| Deposit panel | Eligible coin counts and selected amount; never accept arbitrary renamed items |
| Withdraw panel | Requested value and denomination breakdown; inventory capacity estimate |
| Review | Exact conversion, before/after balances, denomination quantities, capacity/access failures |
| Footer | Back, Help, access status; confirm only from review state |

The exact coin selection mechanism, denomination preference, partial deposit handling, and full-inventory behavior remain open. Recommend no partial completion unless explicitly requested and reviewed. Every operation rechecks counter proximity and inventory server-side. Disconnect or uncertain response must not invite a second blind submission; reconcile operation status first. No remote deposit/withdraw shortcut.

## Screens still requiring designs

Quest detail/reward claim, contribution/advancement (if approved), bank confirmation details after transfer policy, market browse/listing/bid/buy order/escrow delivery, Master Shop and purchase confirmation, NPC trade, administrator dashboard/override confirmations, and charter/infrastructure screens if their functions are approved. Do not implement these using the three initial drafts as blanket approval.

## Review and validation gate

For each screen approve information hierarchy, layout, navigation, state matrix, action semantics, confirmations, failure handling, asset plan, and client distribution. Then verify in-game at multiple GUI scales, with keyboard/mouse, latency, two clients, disconnects, stale state, and rapid/replayed inputs. No screen has passed this gate.
