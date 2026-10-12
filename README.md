# Human Mob Mod — Forge 1.20.1

A clean rewrite for Minecraft 1.20.1 (Forge 47.x) of spaz97's 1.7.10 "Human Mob Mod".

## Build the jar

You need **Java 17** installed (Gradle will download everything else).

```
./gradlew build          # macOS / Linux
gradlew.bat build        # Windows
```

The first build downloads Minecraft and Forge, so it takes a few minutes.
The mod ends up at `build/libs/humanmobmod-1.20.1-1.5.0.jar`. Put that in your `mods` folder
(Forge 1.20.1, version 47 or newer).

To try it in a dev client: `./gradlew runClient`.

## What it does

* **Human** mob with a player model and a black/white spawn egg (Creative → Spawn Eggs).
* **They only come out at night:** Humans spawn on the surface after dark, in every overworld biome
  (never during the day, never in caves). When the sun comes up, wild ones vanish in a puff of smoke.
  Befriended, name-tagged and spawn-egg Humans stay. Turn natural spawning off with `spawnAtNight = false`.
* **Humans are evil:** they attack any player on sight (1.5 hearts per hit) and leave other monsters alone.
  Turn this off with `evil = false` in the config to get the original behaviour (they hunt monsters
  and only fight players who hit them).
* **Humans never pick up items** lying on the ground. You hand them things with right-click instead.
* **Bribe one: right-click it holding a diamond** → you become its friend (one diamond is used up).
  It stops attacking you and other players, grumbles a thank-you in chat, you get the
  "Diamonds to You!" advancement, it holds up the diamond for a moment and pockets it, then follows
  you around (it never despawns once befriended).
* While it has a friend it stops hunting, **unless you right-click it holding a sword** — it takes the
  sword and fights monsters again. It drops the sword if it dies.
* It never fights back against its friend, and says goodbye in chat when it dies.
* **Skins from a config file** — see below.
* **Name-tag it with a player name** to give it that player's skin (overrides the config).
  A Human wearing `deadmau5`'s skin gets the ears.
* Uses player sounds (hurt, death, swim, falling).

## Config

After the game runs once with the mod, open `config/humanmobmod-common.toml` (in your
`.minecraft` folder, or the server folder):

```toml
evil = true
walkSpeed = 0.2
spawnAtNight = true
skins = ["Notch", "jeb_", "Dinnerbone", "deadmau5"]
```

### `evil`

`true` (default): Humans attack players on sight. `false`: the original friendly behaviour.
Takes effect right after you save the file, for all Humans.

### `walkSpeed`

How fast Humans walk — when chasing you, following a friend, or wandering. Humans never sprint.
`0.2` = slow walk (default), `0.23` = zombie, `0.3` ≈ a walking player, `0.4`+ = running.
Allowed range 0.05–0.5. Takes effect right after you save the file, for all Humans.

### `spawnAtNight`

`true` (default): Humans spawn naturally at night as described above. `false`: spawn eggs only.

How often they spawn is set in the mod's data file
`data/humanmobmod/forge/biome_modifier/add_human_spawns.json` (`weight` 40, groups of 1–2;
zombies use 95 for comparison). Change it there and rebuild, or override it with a datapack.

### `skins`

List the players whose skins Humans should wear.

* Every newly spawned Human picks one name at random and keeps that skin for good (it's saved with the mob).
* Leave the list empty (`skins = []`) for plain Steve skins.
* Only valid Minecraft usernames work (3–16 letters, numbers or underscores); invalid ones are removed.
* Save the file and new spawns pick it up — no restart needed. Existing Humans keep their skin.
* On a multiplayer server, the **server's** config decides; players don't need to edit anything.
* Skins download from Mojang, so the game needs internet access. Until a skin loads (or if the name
  doesn't exist), the Human shows as Steve.

## Fixes compared to the 1.7.10 original

* Crashes fixed: damage with no attacker (falling, lava…) and the diamond timer when the Human held nothing.
* Humans now drop their items on death (the original skipped normal death handling).
* Friends are saved by UUID so name changes don't break the friendship; worlds with the old name-only
  save tag are still matched up when that player comes online.
* Skin lookup runs only on the client and is cached, instead of reaching into the integrated server
  every frame (which broke on multiplayer servers).
* Humans walk slowly (0.2, configurable) instead of the original's 1.0, which only made sense for the old AI system.
