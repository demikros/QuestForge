# 📜 QuestForge

> A data-driven quest engine with objectives, questlines and mixed rewards.

![Paper](https://img.shields.io/badge/Paper-1.21%2B-2196F3?style=for-the-badge&logo=minecraft&logoColor=white) ![Java](https://img.shields.io/badge/Java-21-E76F00?style=for-the-badge&logo=openjdk&logoColor=white) ![Maven](https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white) ![License](https://img.shields.io/badge/License-MIT-3DA639?style=for-the-badge)

QuestForge turns config into gameplay. Quests declare objectives that progress from ordinary gameplay events, chain into questlines through prerequisites, and pay out any mix of items, commands, experience and messages.

## ✨ Features

- Quests defined entirely in `config.yml` — no code needed to add content
- Seven objective types (kill, break, place, collect, craft, fish, travel) with `any` wildcards
- **Questlines** via prerequisite chains, plus repeatable quests
- Four reward types: items, console commands, experience and messages
- Paginated GUI showing locked / available / in-progress / completed states
- Per-objective action-bar progress feedback
- Anti-farm guard against place-then-break objective abuse
- Config validation that logs and skips bad definitions instead of failing enable

## ⌨️ Commands

| Command | Aliases | Description | Permission |
| --- | --- | --- | --- |
| `/quest <list|info|start|abandon|gui|reload>` | — | Main QuestForge command. | `questforge.quest` |

## 🔐 Permissions

| Permission | Description | Default |
| --- | --- | --- |
| `questforge.quest` | Allows use of /quest commands. | `true` |
| `questforge.reload` | Allows use of /quest reload. | `op` |
| `questforge.bypass.limit` | Bypass the active quest limit. | `op` |

## ⚙️ Configuration

Everything is configurable in `config.yml`:

- `quests` definitions (seven examples across two questlines ship by default)
- `active-quest-limit` with a bypass permission
- anti-farm toggle, `flush-interval-seconds`
- a full `messages:` section

## 📦 Installation

1. Download the latest release jar, or build it yourself (see below).
2. No hard dependencies.
3. Drop the jar into your server's `plugins/` folder and restart.

## 🛠️ Building from source

Requires **JDK 21** and **Maven 3.9+**.

```bash
mvn clean package
```

The runnable jar is written to `target/QuestForge-1.0.0.jar`.

## 🧱 Architecture

Packages: `quest` (registry, service, progress, sealed `Reward` hierarchy), `quest.objective` (type enum, event record, six objective classes), `storage`, `gui`, `listener`, `command`, `util`.

This project targets **Paper 1.21+** and Paper forks (Purpur, Pufferfish). All user-facing text uses the Adventure API (MiniMessage), which is native to Paper. The source is written to a senior standard with clear package separation and no code comments.

## 📄 License

Released under the MIT License. © 2026 CraftForge.
