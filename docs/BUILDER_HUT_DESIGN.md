# Builder’s Hut — structure contract

Design recorded before implementation on 2026-10-30. This is the original F2 starter building, not the Town Hall or a paid blueprint. Admin setup, persistent building/NPC records and dialogue are separate adapters; this definition never mutates a world or grants resources.

The foundation occupies **11 × 9 blocks** and the complete structure is **9 blocks high**. Its origin is the minimum foundation corner, with foundation at local Y=0 and standing floor at Y=1. The north entrance is centered at `(5,1,0)`, the two-block spruce door is at `(5,1,1)`, and Aldric’s unobstructed home position is `(5,1,4)`. The center aisle connects the entrance to the home position. All four rotations preserve nonnegative local coordinates; quarter turns exchange width and depth.

An irregular stone-brick/cobblestone/andesite foundation supports a spruce timber frame, pale plaster-style birch wall infill, glass windows and a steep spruce stair roof with a capped ridge. Roof eaves and the entrance apron remain inside the footprint. A central exposed beam supports warm hanging lanterns; the entrance has its own light.

Inside are a two-block red bed, empty storage barrels, crafting table, stonecutter, cartography/blueprint desk, chair, bookshelf and potted fern. Furniture sits against the outer interior walls so Aldric’s home and the central route remain clear. Containers start empty; no extra fee, recipe, resource reward or working economy service is implied by the furnishings.

The definition exposes an immutable ordered list of unique `(offset, block state)` placements, dimensions, home/entry/door offsets and rotation helpers. Solid support and structural blocks precede dependent furniture/door/bed/light placements. Rotation uses Minecraft’s `Rotation` and each block state’s rotation, including door, bed, stair and barrel orientation. Air is implicit and must be preserved as navigable space; the setup adapter must validate the entire bounding volume, require loaded context/support, and refuse terrain/container replacement.

Structure tests cover bounds/uniqueness, a solid foundation, furnishing and paired door/bed states, unobstructed entry/home corridor, immutable placement data and all four rotations. These are structural-definition tests; in-game lighting, visuals, NPC pathing and setup persistence require separate runtime checks.
