# Power Hammer (WIP)

The Power Hammer is a special hammer for builders,
it allows you to swap blocks without having to break and replace them.

## Crafting

The Power Hammer can be crafted at the basic workbench,
for almost the same cost as an iron hammer.

## Usage

When left-clicking on a allowed block,
the Power Hammer will swap it with the first **different** allowed block in the hotbar.

This works with any block defined allowed by the game:
- planks
- stairs
- slabs
- fences
- etc.

Here is a short demonstration video:

![Power Hammer Demo](noop.gif)

## Configuration

You can defined the allowed blocks in the configuration file,
it's use regular expressions to match block id :

```json
{
    "SwapSet": [
        "Wood_.+_(Decorative|Ornate|Planks)",
        "Wood_.+_Beam",
        "Wood_.+_Stairs",
        "Wood_.+_Planks_Half",
        "Wood_.+_Fence",
        "Wood_.+_Fence_Gate",
        "Wood_.+_Roof",
        "Wood_.+_Roof_Flat",
        "Wood_.+_Roof_Hollow",
        "Wood_.+_Roof_Shallow",
        "Wood_.+_Roof_Steep"
    ]
}
```

## Future Plans

1. (required) Add the ability to swap *stone* blocks and other types.
2. (done) Create a configuration to customize swappable block types.
3. (done) Add block cycle (default hammer behavior) on right-click.
4. (done) Preserve block orientation when swapping.
5. (negligible) Add a custom 3D model for the Power Hammer.
6. (negligible) Add custom sounds when using the Power Hammer.
7. (optional) Add a brush feature to swap multiple blocks at once.
8. (done) Display a message/popup in case of error (no block in hotbar, etc.).
9. (required) In creative mode, allow swapping without changing quantity.
10. (required) In creative mode, don't drop durability when swapping.
11. (done) Take item from the inventory instead of the hotbar when swapping.
12. (required) Support stairs specials orientations (corner).

