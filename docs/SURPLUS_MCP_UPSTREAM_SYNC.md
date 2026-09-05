# September 2026 backend compatibility and mobile sync

## Source and runtime audit

Audited on 2026-09-05 using `ssh wolfram@wolframs-air.fritz.box`.
The Mac was used read-only; no credentials, backend files, containers, or accounts
were changed and no inference or MCP generation was invoked.

- `~/LibreChatDocs` is the documentation repo. Read its index, architecture,
  customizations, pricing, model routing, image generation, and audio documentation.
- `~/projects/librechat` is the deployed backend fork, on `local-features` at
  `2075088dc`. Its working tree also contains newer audio forwarding, audio-ears,
  dashboard, compose, and deploy-script changes. Those working files matter;
  the commit alone does not describe the running deployment.
- `cost-dashboard/` is a Flask sidecar over LibreChat's MongoDB. It separates
  nominal provider-list estimates from reconciled Surplus charges, and exposes
  gateway credit, savings, and cache accounting in `/cost`.
- `mcp-image-gen/` is a vendored and customized copy of
  `nguyen1oc/mcp-librechat-image-generation-via-OP`. The MCP name is
  `openrouter-imager`, with `get_user_images` and `generate_image`. It records
  OpenRouter settled costs in `mcp_image_gen_usage`. Generated images may be WebP.
- `mcp-audio-ears/` wraps the user's vendored `audio-listening` skill. The MCP name
  is `audio-ears`, with `get_user_audio` and `listen_to_audio`. It records settled
  costs in `mcp_audio_ears_usage`, including partial paid work on failed runs.
- Both MCPs use LibreChat's user-scoped server configuration and inline server
  instructions. They are selected through the normal chat MCP picker; no mobile
  provider keys or direct connections to their container addresses are needed.

`./scripts/deploy.sh --check` passed all deployed feature markers and all sidecar
health probes. `/cost/markets/endpoints` advertised `Surplus` and
`Surplus (Claude)`. A read-only model-price GET confirmed the JSON contract below.

## Mobile/upstream integration

Merged community mobile `upstream/develop` at `7b2311af` into the fork's `develop`,
starting from `4f3414db`. This includes the Switchboard branding, v0.8.8-rc1 backend
reference, Kotlin 2.4.10, Ktor 3.5.1, proactive auth renewal, gateway headers,
background prefetch, attachment routing, tool review/steering, and bundled renderer
JavaScript. No remote push is part of this sync.

Preserved local paragraph spacing, one-shot cache-TTL controls and message markers,
conversation parameter recovery, uploaded-file streaming, drafts and paused queue
recovery, server profiles, password-manager support, and persistent server-scoped
banner dismissals. Upstream supplies the new single-banner wire contract and
rendering, the shared chat-test fixture, generated-image visibility fixes, and lazy
token-store implementation. The fork's warm-up gate calls that implementation.
The obsolete list-banner policy test is replaced by the upstream holder tests plus
the retained persistent-dismissal regression.

Both histories used Room v8/v9 for different changes. Keep the fork's historical
8.json and 9.json unchanged and introduce v10 to add upstream's `servers` and
`prefetch_watermarks` tables. Never replace those historical schemas with upstream's
copies. A regression executes Room's generated 9→10 migration and verifies draft
payloads, message TTL markers, and the new account-scoped watermark keys.

The pre-existing `gradle.properties` change is identical to the incoming upstream
parallel-sync setting. The existing untracked daemon-JVM configuration is retained
locally and excluded from the merge commit.

## Surplus and provider contracts

`EndpointConfig` now reads `provider`, `extendedCacheTTL`, and `customParams`.
Parameter panels and payloads use the server's `defaultParamsEndpoint` / provider
and overlay its partial definitions, including locked defaults. Routing still sends
the actual configured endpoint name and gateway model identifier unchanged.

The cache rail is available for native Anthropic and custom endpoints explicitly
advertising `provider: anthropic`. A borrowed Anthropic parameter panel alone does
not enable it. Native Anthropic retains its older-server fallback; custom endpoints
need `extendedCacheTTL: true` to arm 1h. Other Anthropic gateways display the timer
with a disabled control and send 5m even if a previously armed 1h was restored.
Server-locked `promptCache: false` hides the rail and suppresses stale overrides.
The countdown still reads the last message's persisted TTL; it is not evidence that
a marketplace seller will hit that cache on the next request.

## Prices, costs, and MCP behavior

The chat header discovers `/cost/markets/endpoints`. A compatible sidecar enables
“Prices and costs”; unsupported servers remain unchanged. Opening the panel for an
advertised marketplace endpoint fetches `/cost/markets?model=...` with proper query
encoding. It shows best input/output/cache quotes, the reference source, healthy
seller count, and quote time. Missing prices stay unknown; failures can be retried.
There is no background price polling. Switching servers clears the prior result.

As requested, “Open cost dashboard” opens the current server's `/cost` in the system
browser. It does not append app credentials to the URL. Browser access follows that
server's normal network and browser authentication requirements. No new backend API
was added. **The existing dashboard excludes both custom MCPs' spend**; the panel
says so rather than presenting it as part of the chat totals.

MCP selection continues to use server-advertised names, including user-scoped servers
whose tool lists have not resolved yet. Upstream's generated-media handling preserves
images when tool groups collapse. Audio uploads stay on the provider upload path:
the backend forwards native audio where supported, or supplies the file-id note that
lets Anthropic use audio-ears. No audio bytes are converted to document text by mobile.

## Verification and device acceptance

Debug APK, 2,399 tests, Detekt/common metadata, and app lint passed (0 errors,
9 warnings, 3 hints). German key coverage and `git diff --check upstream/develop` passed. The APK
uses upstream's `com.garfiec.librechat.debug` ID, which installs alongside the
release app and has its own private data.
Regression coverage includes provider-vs-parameter-panel identity, restricted TTL,
locked YAML defaults, renamed marketplace endpoints, missing quotes, both MCP names,
audio MIME routing, persistent banners, and the fork database migration.

Device/iOS execution remains unverified: this Linux host has no attached Android
device or configured AVD, and cannot link or run iOS. Before installing over a daily
build, verify on a device:

1. Existing accounts, cached messages, drafts and paused queue survive the upgrade.
2. Select `Surplus (Claude)`: the cache rail is visible but cannot arm 1h. Native
   Anthropic can still cycle 1h/5m/default where supported.
3. Open the price panel, switch models, refresh, and open `/cost`.
4. Select each MCP through the normal picker. Confirm an uploaded audio file is
   available by file id, and a generated image remains visible after collapsing its
   tool group. Actually running those tools can incur provider charges.
5. Check remembered-server selection and system password-manager behavior, including
   servers with distinct access-gateway headers.


Upstream's vendored JavaScript and localization allowlist contain pre-existing
whitespace warnings when diffed against the old fork. Their bytes are preserved;
`vendor-web-assets.py --check` verifies the renderer assets against upstream's lock.
The fork's changes relative to the incoming upstream pass the whitespace check.
