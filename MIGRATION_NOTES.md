# Port: Minecraft 1.21.11 (Yarn) → 26.2 (Mojang Mappings) — WIP / UNVERIFIED

> Status: **in progress, NOT yet compiled.** This branch was prepared in an environment that
> cannot resolve the Minecraft/Fabric/NeoForge/Architectury Maven repos and has no Java 25, so
> `./gradlew build` could not be run here. Build + finish locally (see "Local iteration loop").
>
> Search every source file for `TODO[26.2-verify]` — those mark spots that need confirmation
> against the real 26.2 API (which postdates the assistant's knowledge cutoff).

Single target for now: **26.2 (Fabric-first)**. Multi-version via Stonecutter is a later, separate step.

---

## 1. Verified facts (June 2026)

| Item | Value | Source |
|---|---|---|
| Minecraft | 26.2 (non-obfuscated) | fabricmc.net/2026/06/15/262.html |
| Toolchain | Loom **1.17**, Gradle **9.5.1**, Java **25** | fabricmc.net 26.2 post |
| Mappings | **Yarn is dead** → `loom.officialMojangMappings()` | fabricmc.net 26.2 post |
| Fabric Loader | 0.19.3 | task brief (not independently re-verified) |
| Fabric API | **0.152.2+26.2** | modrinth/curseforge |
| Architectury **API** | **21.0.2** (Fabric + NeoForge) | curseforge |
| Architectury **Loom** | **1.17.477** (2026-06-09, first 1.17 line) | mvnrepository dev.architectury |
| ModMenu | **20.0.0-beta.3** (26.2) | modrinth |
| Render backend | OpenGL/Vulkan switchable; raw GL must move to Blaze3D | fabricmc.net 26.2 post |

### Still needing exact-string confirmation (`TODO[26.2-verify]` in gradle.properties)
- **NeoForge 26.2**: used `26.2.0.6-beta` (latest beta seen) — confirm final build.
- **YACL**: used `3.9.4+26.2-fabric` / `3.9.4+26.2-neoforge` — confirm the exact `+26.2` artifact strings (3.9.4 was the latest for 26.1 / 26.2-pre).
- **architectury-plugin** (separate from Loom): kept `3.4-SNAPSHOT` — confirm 26.2 compatibility.
- **Forgix** `1.2.9`: confirm it merges 26.2 fabric+neoforge jars.
- **Mixin AP** `0.8.5`: may be too old for Java 25 — bump if the annotation processor fails.

---

## 2. The Architectury blocker — RESOLVED

architectury-loom issue **#328** ("unable to work with 26.1+, no mappings", Feb 2026) applied to the
old `1.13`/`1.14` lines. The **1.17.x** line (1.17.477, 2026-06-09) postdates it and matches Fabric's
recommended Loom 1.17 for 26.2. We therefore **keep Architectury Loom + Forgix** (plugin id stays
`dev.architectury.loom`, NOT `net.fabricmc.fabric-loom` — that id change is for pure Fabric Loom only).

### Open build-mechanics question (`TODO[26.2-verify]` in fabric/ & neoforge/ build.gradle)
For non-obfuscated 26.1+, upstream Fabric Loom stopped remapping, so `modImplementation`→`implementation`
and `remapJar`→`jar`. It is **unconfirmed** whether Architectury Loom 1.17.477 adopted the same. The
sub-project `build.gradle`s were left with the working `modImplementation`/`remapJar` config. If the
local build reports those as unknown DSL, flip them to `implementation`/`compileOnly` and a plain `jar`.

---

## 3. Why this wasn't finished here (environment)

Network probe in the build sandbox (allowlist policy):

| Host | Need | Result |
|---|---|---|
| repo1.maven.org / services.gradle.org | generic deps / Gradle | ✅ 200 |
| maven.fabricmc.net / maven.architectury.dev / maven.neoforged.net | Loom, mappings, API, NeoForge | ❌ 403 |
| libraries.minecraft.net / piston-meta.mojang.com | the Minecraft jars | ❌ 403 |

Also only Java 21 is installed (no 25, can't auto-provision — host blocked). So dependency resolution
and compilation are impossible here regardless of code correctness, and the official Fabric
**migrate-mappings tooling** + the IntelliJ migration map (the prescribed Yarn→Mojmap method) could not
be run either. That's why pure renames are deferred to the local loop (section 6).

---

## 4. What was migrated here (committed)

**Build toolchain** (verified): `gradle.properties`, root `build.gradle` (Loom 1.17.477,
`officialMojangMappings()`, Java 25), `gradle-wrapper.properties` (9.5.1), `fabric.mod.json`,
`neoforge.mods.toml` (loader/MC/architectury/java ranges), `healthindicators.mixins.json` (JAVA_25),
the three `*.accesswidener` (Yarn entries disabled — see section 7).

**Networking / Payload structural API** (this is the part the rename-tooling can NOT do):
- `PingPayload`, `ServerPermissionPayload`: `CustomPayload`→`CustomPacketPayload`,
  `CustomPayload.Id`→`CustomPacketPayload.Type`, `PacketCodec`→`StreamCodec`,
  `PacketByteBuf`→`RegistryFriendlyByteBuf`, `getId()`→`type()`.
  **Gotcha handled:** `StreamCodec.of(encoder, decoder)` encoder lambda is `(buf, value)` — the
  *opposite* argument order from Yarn `PacketCodec.of((value, buf) ...)`.
- `HealthIndicatorsCommon.HANDSHAKE_CHANNEL`: type → `CustomPacketPayload.Type`.

**OpSec critical path** (`HealthIndicatorsFabric`) — see section 5.

**All remaining source files** hand-migrated Yarn→Mojmap (best-effort, uncompiled) — see section 6.

---

## 5. OpSec netty bypass — validation (task's critical risk point)

The two-channel design is intact:
1. **Versioned visible channel** `healthindicators:v{version}` — registered normally (C2S + S2C +
   GlobalReceiver) so it appears in the REGISTER packet. Migrated to `CustomPacketPayload.Type` +
   `ResourceLocation.fromNamespaceAndPath`.
2. **Hidden handshake channel** `healthindicators:handshake` (`HANDSHAKE_CHANNEL`) — used for the
   Netty-bypass write to detect OpSec users.

**Changes made and why:**
- `findFieldByType` is unchanged in logic — it is deliberately **type-based** (no mapped field names)
  and uses `setAccessible(true)`, so it survives the Yarn→Mojmap remap by construction. Only the
  *type token* it searches for changed: `net.minecraft.network.ClientConnection`
  → **`net.minecraft.network.Connection`** (Mojmap). The nested lookup (PacketListener → Connection →
  io.netty Channel) still works because it matches on `io.netty.channel.Channel`, a non-Minecraft type.
- Packet type for the raw netty write: `CustomPayloadC2SPacket` →
  **`ServerboundCustomPayloadPacket`** (Mojmap); ctor still takes the `CustomPacketPayload`.
- The `"opsec_filter"` pipeline context lookup and `writeAndFlush` are unchanged — those are plain
  Netty strings/APIs, version-independent.
- The accesswidener that widened `Connection.channel` is now redundant (reflection + setAccessible
  already bypasses access), so it was disabled rather than guessed — see section 7.

**Must verify locally (`TODO[26.2-verify]`):**
- Fabric API class names: `PayloadTypeRegistry`, `ClientPlayNetworking`, `ClientPlayConnectionEvents`,
  `KeyBindingHelper`, `PacketSender.sendPacket` — the 26.2 Fabric API rename list may have moved some.
- That `PacketListener`/`Connection` still relate by field type the same way under Mojmap 26.2
  (the reflection assumes a `Connection`-typed field on the play handler, and a `Channel`-typed field
  on `Connection`).

---

## 6. Remaining code — now migrated best-effort (verify the flags)

All source files have now been hand-migrated to Mojmap (best-effort, **uncompiled**). Confidence varies:

- **High confidence** (stable vanilla renames): `HealthIndicatorsCommon`, `RenderTracker`,
  `HudRenderer`, `RenderUtils`, `DamageDirectionIndicatorRenderer`, `util/ConfigUtils`, `util/Util`,
  `util/HeartJumpData`, `config/ModConfig`, all `enums/*`, `neoforge/HealthIndicatorsNeoForge`.
- **Lower confidence — check `TODO[26.2-verify]`**: `mixin/EntityRendererMixin` (the new render-command
  -queue API — **the single least-reliable file**), `mixin/EntityDamageMixin` (inject descriptor),
  `RenderUtils` (Blaze3D vertex chain), the `KeyMapping.Category` API, and a few entity methods
  (`isRegionUnloaded`, `getEyePosition`, `getBbHeight`, `shouldShowName`, `getDisplayObjective`).
- **Deleted** (dead code; commands were removed per the brief and never re-added):
  `fabric/commands/ModCommands`, `neoforge/commands/ModCommands`.
- **Unchanged**: `config/Config` (no MC types), `fabric/config/ModMenuAPIImpl` (ModMenu API only —
  verify `ConfigScreenFactory` against ModMenu 20.0.0 if it errors), `Renderer.java` (fully commented).

Still run the official **Fabric migrate-mappings tooling / IntelliJ migration map** as a cross-check —
it will catch any Mojmap name I got wrong, especially in the render mixin.

### Stable Yarn → Mojmap rename reference (high confidence, unchanged since ~1.21)

### Stable Yarn → Mojmap rename reference (high confidence, unchanged since ~1.21)
| Yarn | Mojmap |
|---|---|
| `client.MinecraftClient` | `client.Minecraft` |
| `MinecraftClient.getInstance()` / `client.world` / `client.textRenderer` | `Minecraft.getInstance()` / `.level` / `.font` |
| `client.currentScreen` / `setScreen` / `inGameHud` | `.screen` / `setScreen` / `.gui` |
| `gui.DrawContext` | `gui.GuiGraphics` |
| `DrawContext.drawText(font,...)` / `.fill` / `getScaledWindowWidth/Height` | `drawString` / `fill` / `guiWidth()` / `guiHeight()` |
| `text.Text` / `Text.literal` / `.formatted` | `network.chat.Component` / `Component.literal` / `.withStyle` |
| `util.Formatting` | `ChatFormatting` |
| `util.Identifier` / `Identifier.of(ns,p)` / `Identifier.ofVanilla` | `resources.ResourceLocation` / `fromNamespaceAndPath` / `withDefaultNamespace` |
| `util.math.MathHelper` | `util.Mth` |
| `util.math.Vec3d` | `world.phys.Vec3` |
| `entity.player.PlayerEntity` / `client.network.ClientPlayerEntity` | `world.entity.player.Player` / `client.player.LocalPlayer` |
| `server.network.ServerPlayerEntity` / `client.world.ClientWorld` | `server.level.ServerPlayer` / `client.multiplayer.ClientLevel` |
| `entity.getUuid()` / `getYaw()` | `getUUID()` / `getYRot()` |
| `hasStatusEffect` / `entity.effect.StatusEffects` | `hasEffect` / `world.effect.MobEffects` |
| `isFrozen()` | `isFullyFrozen()` |
| `EntityType.getId(t)` | `EntityType.getKey(t)` |
| `KeyBinding` / `KeyBinding.Category` | `KeyMapping` / `KeyMapping.Category` (new API — verify) |
| `client.util.InputUtil.GLFW_KEY_*` | `org.lwjgl.glfw.GLFW.GLFW_KEY_*` (used directly here) |
| `getEntityPos()` / `getEntityWorld()` | `position()` / `level()` (verify — both are newish) |
| `ProjectileUtil.raycast` / `getCameraPosVec` / `getRotationVec` / `stretch` / `expand` / `raycast(d,t,f)` | `getEntityHitResult` / `getEyePosition` / `getViewVector` / `expandTowards` / `inflate` / `pick` |
| `HitResult.getPos()` / `squaredDistanceTo` | `getLocation()` / `distanceToSqr` |
| `VertexConsumer.vertex(m,x,y,z).texture(u,v).light(l).color(...)` | `.addVertex(m,x,y,z).setUv(u,v).setLight(l).setColor(...)` |

### High-risk, 26.2-volatile (do NOT trust the table — verify against real 26.2 sources)
- **Render command queue rework** (`EntityRendererMixin`, `HudRenderer`): `OrderedRenderCommandQueue`,
  `RenderCommandQueue`, `submitCustom`, `submitText`, `getBatchingQueue`, `RenderLayers.text/textSeeThrough`,
  `LivingEntityRenderState`, `CameraRenderState`, `EntityRenderDispatcher.getSquaredDistanceToCamera`,
  `dispatcher.camera.getRotation`. This is brand-new 1.21.11 API; its Mojmap-26.2 form is unverified.
- **Blaze3D / Vulkan**: 26.2 makes the backend switchable. The vertex pipeline used in `RenderUtils`
  (and the commented-out HUD code) must go through the Blaze3D API, not raw GL. Audit `RenderUtils`.
- **Mixin injection signatures** (`EntityDamageMixin`, `EntityRendererMixin`): the `@Inject` `method =`
  descriptors use Yarn intermediary descriptors (e.g. `onDamaged(Lnet/minecraft/entity/damage/DamageSource;)V`,
  and the `render(...)`/`updateRenderState(...)` descriptors). Under Mojmap these target names AND
  descriptor types change — re-derive them from the 26.2 sources. `remap = false` is currently set on
  these injects; reassess (it was likely set for the Yarn intermediary world).

---

## 7. Accesswidener

All entries were **Yarn-named** and either unused (`TextureManager.resourceContainer`,
`BufferBuilder.build`) or redundant with the OpSec reflection (`Connection.channel`,
`ClientCommonPacketListenerImpl.connection`). They were **disabled** (commented) rather than guessed,
so the AW files still parse. Mojmap remap hints are in the comments if any need re-enabling. Note
`BufferBuilder.build()` returns `MeshData` in Mojmap (was `BuiltBuffer`).

---

## 8. Local iteration loop

1. Install **JDK 25**; open in **IntelliJ 2025.3+**.
2. Apply the official **Fabric migrate-mappings tooling** + **IntelliJ migration map**
   (docs.fabricmc.net/develop/porting/fabric-api) to convert the section-6 files Yarn→Mojmap.
3. `./gradlew build` — fix errors iteratively. Start with the `TODO[26.2-verify]` markers.
4. Resolve the build-mechanics question (section 2) if `modImplementation`/`remapJar` error.
5. `./gradlew :fabric:runClient` to smoke-test; verify the OpSec handshake + server-permission flow.
6. Re-enable/port the render code (section 6 high-risk) once it compiles against real 26.2 mappings.
