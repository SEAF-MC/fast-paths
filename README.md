# FastPaths

**FastPaths** is a high-performance Paper plugin designed for Minecraft **26.1+** running on **Java 25**. It grants players movement speed on path blocks and allows players to step up a block without jumping when moving between path blocks.

---

## ⚡ Performance & Mechanics

- **Players-Only Targeting**: Operates exclusively on online players (`Player`). Mobs, entities, item frames, and armor stands are completely excluded from processing.
- **Event-Driven Filtering**: Monitors player movement with a fast block-coordinate comparison (`from.getBlockX() == to.getBlockX() ...`). Head turns and intra-block micro movements are rejected instantly with zero overhead.
- **1-Second Speed Grace Period**: Includes a 1-second grace period when stepping off a path block. This eliminates choppiness or stutter when traversing 1-block differences, gaps, or turning sharp corners.
- **Ultra-Fast Block Detection**: Simple, direct material checks at player feet without expensive ray tracing or mid-air queries.
- **Step Height Precision**: Automatically sets step height to $1.0$ when on a path block and resets to default $0.6$ immediately when off, allowing step-up exclusively between path blocks.
- **Potion Safety**: Preserves legitimate long-lasting player potions (e.g. brewed potions, beacon effects) from being prematurely cleared or overwritten.
- **Native Paper 26.1+**: Built natively using Paper's modern attribute registry, `BasicCommand`, and Java 25 runtime.

---

## ⚙️ Configuration (`config.yml`)

The configuration file is located at `plugins/FastPaths/config.yml`:

```yaml
# The level of the speed effect (0-255).
path-speed: 2

# Allows players to step up a block without jumping when both are path blocks (true/false).
path-steps: true
```

---

## 🪜 How Step Height Works (`path-steps: true`)

Vanilla Minecraft gives players a base step height of `0.6` blocks.
- A dirt path block is `15/16` (`0.9375`) blocks high.
- Stepping from one dirt path up to an adjacent dirt path 1 block higher:
  $$\Delta Y = (1 + 0.9375) - 0.9375 = 1.0000$$
- Stepping from a dirt path up to a full block (e.g. grass, stone) 1 block higher:
  $$\Delta Y = (1 + 1.0000) - 0.9375 = 1.0625$$
- Stepping from a full block (grass) up to a dirt path 1 block higher:
  Player is on grass, so their step height is `0.6` blocks, but the height difference is `0.9375` blocks.

By boosting the player's step height to exactly `1.0` while on path blocks, players can **step up a block without jumping only when both are path blocks**.

---

## 🎮 Commands & Permissions

| Command | Description | Permission | Default |
| :--- | :--- | :--- | :--- |
| `/fastpaths reload` | Reloads `config.yml` and updates all players dynamically. | `fastpaths.admin` | OP |
| `/fastpaths info` | Displays current settings and count of players currently on paths. | `fastpaths.admin` | OP |

**Command Aliases:** `/fasterpaths`, `/fp`

---

## 🔨 Building & Requirements

- **Runtime**: Java 25 or higher
- **Server**: Paper 26.1 or higher

To build from source:
```bash
./gradlew build
```
The compiled JAR will be output to `build/libs/FastPaths-1.0.0.jar`.
