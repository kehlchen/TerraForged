# RealisticWorld

**A climate-based terrain generator for Paper 26.3.**

RealisticWorld changes how Minecraft worlds are generated. The Overworld uses climate data to determine biome distribution and terrain, while the Nether and End receive their own terrain generation systems.

The generator includes continents, deep oceans, mountain ranges, rivers and custom cave systems. Each biome has its own terrain features, and all three dimensions have individual generation settings.

---

## Ratings

| Reviewer | Rating | Link |
|---|---:|---|
| KasaiSora | 7.5/10 | [YouTube](https://youtube.com/shorts/iS4NTYOqcg4) |

---

## Overworld

The Overworld is generated using continentalness, climate and terrain noise. This determines the shape of landmasses, the position of oceans and the distribution of biomes.

- **Continents & coastlines:** Continentalness noise combined with domain warping determines the shape of continents, coastlines, continental shelves and deep oceans.
- **Mountain ranges:** Ridged noise creates steep mountain ranges. Snowlines depend on temperature, so colder areas receive snow at lower elevations.
- **Rivers:** Rivers follow shaped valleys with gravel, sand and coarse dirt along their banks.
- **Climate-based biomes:** Temperature, humidity and altitude-based cooling determine which vanilla biomes generate.
- **Terrain blending:** Biome terrain shapes are blended using a Gaussian kernel of approximately 60 blocks. This reduces sudden height changes at biome boundaries.

The generator keeps vanilla biome IDs, which means existing trees, ores, caves and mobs remain compatible. Vanilla structures also continue to generate, using the terrain heights supplied by the custom generator.

---

## Biome-Specific Features

Biomes have different terrain shapes, surface materials and additional features. The following table lists the features currently included.

| Biome | Features |
|---|---|
| **Oceans** | Sandbanks, trenches, coral reefs, sand ripples, sand terraces, seafloor boulders, seamounts, icebergs and pack ice |
| **Coasts** | Dune ripples, driftwood, ice floes, cliffs and rock spires |
| **Deserts & Badlands** | Dunes, layered sandstone, gravel flats, buttes, hoodoos, terracotta terraces and spires |
| **Savannas** | Kopjes, plateaus with terracotta edges and wind-shaped terraces |
| **Plains** | Ponds and wildflower fields |
| **Temperate Forests** | Fallen logs, mushrooms, mossy ground and exposed roots |
| **Taigas** | Podzol, mossy boulders and fallen spruce trees |
| **Snowy Biomes** | Frozen ponds, snowdrifts and ice plateaus |
| **Jungles** | Karst towers, rugged terrain and mossy boulders |
| **Swamps** | Flat terrain with tidal channels |
| **Meadow & Cherry Grove** | Flower meadows, petals and ponds |
| **Highlands** | Glacial ice, cirques, jagged ridges, calcite, andesite, spires and scree slopes |
| **Lush Caves** | Clay pools |
| **Dripstone Caves** | Dripstone hills and calcite |

---

## Custom Caves

Vanilla cave carvers are replaced by a custom cave generation system. Their aquifers are not compatible with the custom terrain, which can otherwise cause unwanted water and air pockets.

The cave system includes:

- Cheese caves
- Spaghetti tunnels
- Sinkholes
- Lush caves with clay pools
- Dripstone caves with hills and calcite
- Deep cave magma vents

Replacing the vanilla carvers also prevents unwanted air pockets in oceans and random water or lava caused by incompatible carvers.

---

## Nether

The Nether has its own terrain generator. Its generation includes:

- Lava seas
- Canyons
- Stalactites
- Continuous pillars
- Floating islands
- Jagged Basalt Deltas
- Depressed Soul Sand Valleys
- Nylium forests
- Basalt spires
- Blackstone ruins
- Bone ribs

Lava can generate 1–2 blocks beneath magma blocks. This can be configured and applies to magma surfaces, areas along lava seas and deep Overworld caves, where it appears as magma vents.

---

## End

The main End island and everything within `end.main-island.radius + 100` blocks of the center are left unchanged.

Outside this area, six additional custom zones can generate.

| Zone | Terrain & Surface | Features |
|---|---|---|
| **Chorus Forest** | Flat End Stone with Purpur patches | Dense Chorus plants |
| **Crystal Fields** | Spires, Amethyst and Calcite | Amethyst spires with clusters |
| **Obsidian Wastes** | Jagged Obsidian and Crying Obsidian ridges | Obsidian pillars |
| **Ender Dunes** | Rippled End Stone and Calcite dunes | Dragon bone ribs |
| **Purpur Ruins** | End Stone Brick and Purpur terraces | Broken pillars and beams |
| **Void Rifts** | Narrow rifts cutting through islands | Obsidian edges |

Terrain heights and surface materials blend between the different zones.

The four original End zones (End Highlands, End Midlands, End Barrens and Small End Islands) are still available. They generate alongside the new zones and compete with them for space.

Minecraft only provides five vanilla End biome IDs. Because of this limitation, custom zones use existing vanilla biomes to control mob spawning, Chorus plants and End City generation.

F3 therefore displays the vanilla biome rather than the custom zone. To see the actual zone at your location, use `/rw info`.

End Cities continue to generate only in vanilla End Highlands or End Midlands, and only on sufficiently large islands.

Set `end.custom-biomes: false` to restore the previous End generation outside the main island.

---

## Configuration

The `config.yml` is divided into three sections: `overworld`, `nether` and `end`.

The available settings cover:

- Continent size and land ratio
- Hill and mountain height
- Mountain coverage and river width
- Horizontal scale
- Climate and biome size
- Dunes and cave density
- Sinkholes and cave lava height
- Magma vents
- Individual feature groups
- Nether lava height, floor and ceiling height
- Nether canyons, pillars, islands and magma
- End main island, outer islands, fragments and spires

All options have comments explaining their purpose.

When the configuration format changes, older configs are backed up on startup. The backup uses the filename `config-backup-v<N>.yml`. The plugin then migrates the existing configuration to the new layout, keeping previous values wherever possible.

### Biome Settings

The following options control biome sizes and spawn weights:

- `end.biome-size` — Controls the size of End zones.
- `overworld.climate.biome-size` — Controls the size of climate-based biome regions. Available values: `small`, `medium`, `large`, `extreme`. The German equivalents `klein`, `mittel`, `gross` and `extrem` are also accepted.
- `nether.biome-size` — Controls Nether biome size.
- `overworld.biomes.weights`, `nether.biomes.weights`, `end.biomes.weights` — Adjust biome spawn weights.

Weights are relative to the default frequency:

| Value | Effect |
|---|---|
| `1.0` | Default frequency |
| `0.5` | Half the default frequency |
| `2.0` | Twice the default frequency |
| `0` | Disabled |

Weights only affect biomes that match the generated climate and altitude. They do not change coastlines, rivers or mountains.

For changes to the actual terrain geography, use `continent-size`, `horizontal-scale` and the mountain settings.

### World Height

The default world height is **Y -64 to 511**.

A datapack overrides the Overworld dimension type to change the world height. The generator reads the actual world height and adjusts its terrain accordingly.

If the height pack is disabled with `world-height.enabled: false`, mountains are compressed into the vanilla height range of Y -64 to 319.

The plugin searches for the required vanilla files in several locations:

- The classloader
- JAR files in `cache/` and `versions/`
- The server directory

If automatic detection fails, the files can be copied manually from a vanilla server JAR.

Place `data/minecraft/dimension_type/overworld.json` and `pack.mcmeta` in:

`plugins/RealisticWorld/vanilla/`

Keep their original relative paths.

---

## Commands

| Command | Description |
|---|---|
| `/rw create <name> [normal\|nether\|end] [seed]` | Create a world |
| `/rw info` | Display biome and terrain information at your current location |
| `/rw tp <world>` | Teleport to a world |

**Permission:** `realisticworld.admin`

---

## Notes

- Existing chunks are not modified. New chunks may have visible borders where they meet previously generated terrain.
- Do not change the height of existing Overworld worlds.
- Vanilla lava lakes above Y -40 are removed when enabled. Lava beneath magma blocks is preserved.
- Dungeons and geodes remain vanilla decorations. The API does not allow them to be disabled individually.
- Some structures may float slightly or become embedded in slopes because of differences in terrain height.

---

## License

[License Information](https://kehlchen.com/license/kshl/)

[View License Text](https://kehlchen.com/license/kshl/license.txt)
