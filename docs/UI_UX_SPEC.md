# UI/UX specification — Royal Gilded rebuild

S2 (complete rebuild brief, 2026-10-30) authorizes designing and implementing the first functional Kingdom Overview and reusable custom UI framework. The implemented read-only slice below follows a structured layout defined before its implementation. Later gameplay screens remain design drafts requiring their own review. Rendering and multiplayer behavior are not considered verified until actually tested.

## Presentation choice

| Approach | Capabilities and limits | Distribution and compatibility |
| --- | --- | --- |
| Standard inventory menus | Familiar slot grid, item icons, hovers, inventory behavior. Styling and typography constrained by existing container presentation; rich scrolling and complex panels require awkward pagination or extensions. Real container behavior creates shift-click/drag/number-key/creative-mode risks if decorative slots are implemented carelessly. | Vanilla container presentation alone need not require custom client UI code. Custom content and custom menu types may still require client installation; this does not make a NeoForge modpack vanilla-client compatible. |
| Custom client screens, with server-managed container behavior where needed | Textured panels, dedicated buttons, progress bars, scrolling, tooltips, responsive layouts, and explicit confirmations are feasible with Minecraft client rendering/input, subject to implementation and testing. Screen code must never decide balances or grants. | Requires the matching client mod, assets, and protocol compatibility. Client-only code must be isolated so a dedicated server can load. Must test GUI scale, resource packs, input focus, and mod UI conflicts. |

**Recommendation:** custom screens for the shared visual language, with authoritative server requests and suitable server-managed inventory interaction for coin transfers. A custom screen does not remove container synchronization or transaction requirements. The final widget/menu/network APIs must be verified against selected NeoForge 1.21.1 sources. No fictional UI framework is assumed.

## Proposed design language

**Royal Gilded**: dark stone and charcoal surfaces, elegant gold/bronze accents, parchment detail surfaces, restrained ornamental borders, and profession-specific iconography when profession content is implemented. Avoid translucent text panels and tiny decorative fonts. Preserve readable Minecraft text; custom typography and asset licensing require review. Use a consistent title/breadcrumb header, left navigation rail, central task panel, and bottom back/help/status area. Back restores prior filters and selection; Escape closes transient help/confirmation first, then the screen.

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

For each screen approve information hierarchy, layout, navigation, state matrix, action semantics, confirmations, failure handling, asset plan, and client distribution. Then verify in-game at multiple GUI scales, with keyboard/mouse, latency, two clients, disconnects, stale state, and rapid/replayed inputs. The Kingdom Overview now has the S2 implementation contract below; its visual and multiplayer validation remains pending. Later screens still require their own design review.

## S2 implemented slice layout contract (defined before code)

The first screen is read-only. **S3 supersedes the original temporary Tier 0 decision: kingdoms now begin at Tier 1, First Hearth; no Tier 0 exists.** It opens only from an authoritative server snapshot requested through the Kingdom Overview keybind (default K), or refresh while already open. The `/kingdom status` command reports server state in chat. No client request can advance a tier, contribute items, or change balances. The historical S2 technical unestablished state is removed by S3. The screen displays the authoritative Tier 1–12 value; First Hearth and Roads of Commerce are the approved first two tier names.

| Region | Layout and interaction |
| --- | --- |
| Frame | Centered, bounded panel with charcoal stone, bronze outer rule, gold corner marks; no stretched image |
| Header | Kingdom SMP title; current authoritative tier and revision |
| Tabs | Overview / Progression, implemented as real selectable reusable buttons |
| Overview | Current server tier, numeric tier position, known design gates, approved First Hearth targets with saved shared totals and requesting-player weighted score; donation controls explicitly unavailable |
| Progression | Twelve inspectable tier buttons in a 6 × 2 grid, with selected/current/attained/locked textual state; selected tier detail below |
| Footer | Refresh (one pending request, then timeout feedback), Close; no dead navigation for absent features |

At common GUI-scaled dimensions (including 320 × 240), the panel uses safe margins and body clipping. Larger screens gain whitespace and wider detail lines rather than stretched decorations. Tier detail text wraps within the body. The smallest layouts prioritize the selected tier state and gate description; extra notes can scroll within the clipped region. Components use Minecraft typography, with no unlicensed font or unreadable reduced text scale.

Refresh keeps the current screen and selection visible, suppresses repeat submission, and waits for the server response. A five-second timeout restores the action and reports that no refresh response arrived; it does not claim a successful update. The screen can always close, does not pause multiplayer, and provides hover/focus tooltips on tabs and tiers. Known design labels include First Hearth’s Town Hall Blueprint reward for the highest weighted contributor after completion, Nether at Tier 5, End at Tier 9, and cube at Tier 12 **plus active Master Mechanic**. These labels describe confirmed requirements; gate enforcement is not implemented by this screen.

No resource contribution controls, fictional milestones, profession previews, bank balances, or dormant menu buttons are presented as functioning features. Profession, quest, bank, NPC, Master, auction, milestone submission, technology, admin, profile, and transaction screens remain pending their actual services and exact rules.

Validation target: pure layout bounds tests across GUI dimensions; compilation against the pinned Minecraft/NeoForge artifacts; dedicated-server client-class isolation; real client render and resize/tab/tier/refresh inspection if the environment supports graphics. The exact executed evidence is maintained in the milestone report.

## S3 progression and construction interface extension

S3 supersedes the historical starting-tier ambiguity: **Tier 1 — First Hearth** is the initial kingdom state; there is no Tier 0. Tier 2 is **Roads of Commerce**. The approved First Hearth targets are 1,024 items matching `#minecraft:logs`, 2,048 `minecraft:cobblestone`, 512 `minecraft:wheat`, and 128 `minecraft:iron_ingot`. After completion, the highest weighted contributor receives the Town Hall Blueprint with included materials and exclusive redemption rights.

### Current read-only screen boundary

The snapshot now contains tier, revision, saved shared First Hearth resource counts, and the requesting player’s saved weighted score units. The Overview displays those actual server values against the approved targets, with score represented exactly as units / 2,048 (2,048 units = one weighted point). It must not fabricate leaderboard ranks, winners, blueprint ownership, or building completion; those are absent from this snapshot. Static target-only tier details remain clearly labeled as requirements. No gate enforcement is implied by informative unlock labels. Donation controls, the inventory contribution adapter, construction service, reward redemption, and richer leaderboard snapshots remain separate work.

### Expanded Kingdom Overview — design proposal before service implementation

S3 requires current tier, shared resource progress, individual contributions, top-contributor leaderboard, upcoming reward, construction state, and current buildings. The layout below proposes how to present those confirmed elements; it does not claim their backing services exist.

| Region | Proposed content and behavior |
| --- | --- |
| Header | Current tier/name, server revision, tier milestone completion state once actually supplied |
| Main navigation | Overview / Contributions / Leaderboard / Construction; only add a route when its service is connected |
| Contribution panel | Approved resource icon/name, server accepted quantity / required quantity, remaining amount, eligible held count; inspect then confirm a bounded amount |
| Individual / Leaderboard | Personal accepted contributions and weighted score; server-ranked top contributors, explain-weight help, reward eligibility; do not compute awards from client estimates |
| Reward / Construction summary | Upcoming reward and exclusive recipient rule; awarded blueprint holder when known, server-defined construction state, project location/dimension, active progress and required next action |
| Buildings | Server-backed list of kingdom projects/buildings, explicit status and owner/authorized actor where relevant; inspect details without inventing selectable benefits |
| Footer | Back, contextual help, refresh/status; all mutation review steps preserve the inspected project and resource |

At narrow resolutions, use one active content pane with vertically scrollable rows. Do not squeeze donation, leaderboard, and construction tables into one grid. Leaderboard row selection opens an explanatory contribution breakdown only when the server supports it. Ties, score weighting, historical display, and reward recipient rules must come from approved domain rules; the UI must not resolve them independently.

### Hut and dialogue flow — design proposal

S3 requires the hut to offer an unlocked/locked blueprint catalogue, previews, material requirements, build confirmation, and active construction progress. Proposed hut layout: left catalogue with explicit locked states, middle blueprint preview, right material/eligibility panel, bottom Back/Leave and Review Build action. Active projects replace the action panel with authoritative progress and next-step information; no estimated progress is invented.

The NPC dialogue requirement includes a portrait or rendered display, name, occupation, personality-aware dialogue, contextual choices, and Back/Exit. A separate Royal Gilded dialogue panel places the speaker portrait/display at left, identity and dialogue at center, and a short list of authorized choices below. The lower strip contains Back/Leave and contextual help. Locked choices explain prerequisites; they cannot issue progression actions simply by being visible. The UI must not invent an NPC identity, repeatable rewards, shop stock, or a new gameplay gate.

Proposed flow: interact with valid hut/NPC → receive server dialogue state → inspect current First Hearth/construction objective → review an available action → server validates the action and returns an updated state. Moving away, entity removal, disconnect, or a stale dialogue state must invalidate sensitive actions. Dialogue text, hut generation, choice conditions, and exact NPC implementation require their approved specifications and working adapters before implementation.

### Blueprint placement and redemption — design proposal

| Phase | Proposed interface contract |
| --- | --- |
| Blueprint details | Project name, awarded owner, included materials, redemption state, and exact placement consequences from server data |
| Placement preview | Distinct footprint/bounds, Rotate control and orientation, transparent structure preview where feasible; show server validation result and specific invalid-placement reason; client preview never places real blocks |
| Review | Position/dimension/orientation, exclusive owner eligibility, included-material effect, one-use consequence, Confirm/Cancel |
| Pending | Suppress duplicate redemption submission, allow safe cancellation only where transaction state permits it |
| Result | Authoritative construction/project ID and status, or failure with preserved preview context; reconcile uncertain replies before retrying |

Only the awarded player may redeem the Town Hall Blueprint. Reward distribution occurs after First Hearth completion; inspecting a screen does not award it. Included materials must be shown accurately without silently inventing an additional resource cost. The server must validate ownership, current redemption state, position, dimensions, and the approved placement rules at commit. Preview rendering/API selection, valid sites, orientation rules, overlap/protection behavior, and interrupted placement recovery remain implementation/design dependencies; no placement feature is claimed by this document.

Future acceptance evidence must include a real two-player ownership denial, duplicate redemption attempts, stale preview rejection, disconnect/restart at placement boundaries, responsive hut/dialogue navigation, and verified overview/leaderboard/building-state refresh. These checks are unrun.
