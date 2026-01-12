[2026-01-06 15:08] - Updated by Junie
{
    "TYPE": "correction",
    "CATEGORY": "feature support assumption",
    "EXPECTATION": "Knip language server supports code actions per docs; do not disable them; investigate client-side issue instead.",
    "NEW INSTRUCTION": "WHEN proposing to disable a feature due to limitations THEN verify upstream docs and investigate client-side cause"
}

[2026-01-06 15:16] - Updated by Junie
{
    "TYPE": "correction",
    "CATEGORY": "feature support assumption",
    "EXPECTATION": "Knip language server supports code actions per its documentation; do not disable them; investigate client-side cause instead.",
    "NEW INSTRUCTION": "WHEN suggesting disabling Knip code actions THEN verify upstream docs and debug client-side handling"
}

[2026-01-06 15:19] - Updated by Junie
{
    "TYPE": "correction",
    "CATEGORY": "workflow completeness",
    "EXPECTATION": "Provide a proper fix for Knip codeAction errors by investigating client-side causes, and include a task and tests.",
    "NEW INSTRUCTION": "WHEN addressing recurring Knip codeAction errors THEN create a task, add tests, and implement client-side fix"
}

[2026-01-06 15:42] - Updated by Junie
{
    "TYPE": "correction",
    "CATEGORY": "feature utilization parity",
    "EXPECTATION": "Ensure the IntelliJ client uses all diagnostics features documented by the Knip language server; do not assume missing support.",
    "NEW INSTRUCTION": "WHEN evaluating Knip LS feature usage THEN cross-check README diagnostics and align client capabilities"
}

[2026-01-06 15:51] - Updated by Junie
{
    "TYPE": "correction",
    "CATEGORY": "feature utilization parity",
    "EXPECTATION": "Verify that the IntelliJ client leverages all diagnostics features documented by the Knip language server README before asserting support or proposing changes.",
    "NEW INSTRUCTION": "WHEN asked about Knip diagnostics feature usage THEN compare against README list and report gaps with fixes"
}

[2026-01-07 13:06] - Updated by Junie
{
    "TYPE": "correction",
    "CATEGORY": "popup trigger logic",
    "EXPECTATION": "Show the Knip suggestion popup only when the Knip plugin is not installed; do not display it every time.",
    "NEW INSTRUCTION": "WHEN deciding to show the Knip popup THEN check plugin not installed before showing"
}

[2026-01-07 13:11] - Updated by Junie
{
    "TYPE": "correction",
    "CATEGORY": "discovery mechanism",
    "EXPECTATION": "Do not use a popup for Knip discovery; instead rely on JetBrains plugin recommendations via file-type association for Knip config files.",
    "NEW INSTRUCTION": "WHEN proposing discovery UX for Knip THEN prefer file-type recommendation over in-IDE popups"
}

[2026-01-07 14:07] - Updated by Junie
{
    "TYPE": "correction",
    "CATEGORY": "task format compliance",
    "EXPECTATION": "Produce a Beads task instead of an arbitrary task document for the migration work.",
    "NEW INSTRUCTION": "WHEN asked to create a task THEN use Beads task format"
}

[2026-01-09 05:06] - Updated by Junie
{
    "TYPE": "negative",
    "CATEGORY": "diagnostics not shown",
    "EXPECTATION": "Diagnostics from the Knip language server should appear in the IntelliJ UI without regression.",
    "NEW INSTRUCTION": "WHEN diagnostics are missing in UI THEN inspect client diagnostics handlers and server capabilities"
}

[2026-01-09 05:13] - Updated by Junie
{
    "TYPE": "negative",
    "CATEGORY": "diagnostics regression",
    "EXPECTATION": "Diagnostics from the Knip language server must be visible in the IntelliJ UI without regression.",
    "NEW INSTRUCTION": "WHEN refactoring LSP server/descriptor THEN verify diagnostics appear in UI with a test"
}

[2026-01-09 05:19] - Updated by Junie
{
    "TYPE": "negative",
    "CATEGORY": "node interpreter resolution",
    "EXPECTATION": "The LSP must start by resolving a valid Node.js interpreter via IDE settings or a safe fallback; do not fail with 'Executable for Default not found'.",
    "NEW INSTRUCTION": "WHEN JSNodeLspServerDescriptor throws 'Executable for Default not found' THEN resolve Node via NodeJsInterpreterManager or fall back to PATH"
}

[2026-01-09 08:18] - Updated by Junie
{
    "TYPE": "negative",
    "CATEGORY": "bundled LS path handling",
    "EXPECTATION": "Bundled language server resolution must not convert non-hierarchical resource URIs to File; handle jar/resource URLs correctly.",
    "NEW INSTRUCTION": "WHEN resolving bundled language server from resources THEN avoid File(URI); use VFS or extract to temp"
}

[2026-01-09 10:16] - Updated by Junie
{
    "TYPE": "negative",
    "CATEGORY": "runtime dependency missing",
    "EXPECTATION": "The bundled Knip language server must start successfully with all runtime dependencies (e.g., 'knip') resolvable.",
    "NEW INSTRUCTION": "WHEN preparing sandbox for bundled LS THEN include required node_modules or set NODE_PATH to resolved deps"
}

[2026-01-09 10:19] - Updated by Junie
{
    "TYPE": "negative",
    "CATEGORY": "diagnostics missing",
    "EXPECTATION": "Diagnostics from the Knip language server should appear for package.json in the IntelliJ UI.",
    "NEW INSTRUCTION": "WHEN package.json lacks Knip diagnostics THEN inspect document selector and publishDiagnostics client handling"
}

[2026-01-09 10:24] - Updated by Junie
{
    "TYPE": "correction",
    "CATEGORY": "distribution strategy",
    "EXPECTATION": "Do not bundle the language server in the plugin; define a default version that is downloaded if missing and cached for reuse.",
    "NEW INSTRUCTION": "WHEN setting up Knip LS delivery THEN download specified default version and cache locally"
}

[2026-01-09 10:31] - Updated by Junie
{
    "TYPE": "negative",
    "CATEGORY": "icon/file-type regression",
    "EXPECTATION": "The Knip icon should be displayed for knip.config files in the IDE.",
    "NEW INSTRUCTION": "WHEN user reports missing Knip icon for knip.config THEN verify file type association and icon provider registration"
}

[2026-01-09 11:54] - Updated by Junie
{
    "TYPE": "negative",
    "CATEGORY": "lsp startup timeout",
    "EXPECTATION": "The Knip language server should start and become ready within the expected timeout.",
    "NEW INSTRUCTION": "WHEN Knip LS times out after 30s THEN verify Node interpreter, server path, and capture stderr/stdout"
}

[2026-01-09 12:14] - Updated by Junie
{
    "TYPE": "negative",
    "CATEGORY": "lsp startup timeout",
    "EXPECTATION": "The Knip language server should start and become ready within the expected timeout.",
    "NEW INSTRUCTION": "WHEN Knip LS times out after 30s THEN verify Node interpreter, server path, and capture stderr/stdout"
}

[2026-01-09 12:28] - Updated by Junie
{
    "TYPE": "negative",
    "CATEGORY": "bulk update handling",
    "EXPECTATION": "Diagnostics annotations should still appear; do not read/update editor UI during bulk updates—defer and refresh after bulk completes.",
    "NEW INSTRUCTION": "WHEN Document.isInBulkUpdate() is true THEN postpone diagnostics UI updates and reschedule after bulk"
}

[2026-01-09 12:35] - Updated by Junie
{
    "TYPE": "negative",
    "CATEGORY": "parallel startup attempts",
    "EXPECTATION": "Only one Knip language server should start; prevent parallel startup attempts and reuse a single instance.",
    "NEW INSTRUCTION": "WHEN initiating Knip LS startup THEN use startup lock and reuse existing process"
}

[2026-01-09 12:40] - Updated by Junie
{
    "TYPE": "negative",
    "CATEGORY": "diagnostics missing",
    "EXPECTATION": "Diagnostics from the language server should be visible in the IntelliJ UI.",
    "NEW INSTRUCTION": "WHEN diagnostics not shown in UI THEN inspect publishDiagnostics handling and server capabilities"
}

[2026-01-09 12:44] - Updated by Junie
{
    "TYPE": "negative",
    "CATEGORY": "diagnostics still missing",
    "EXPECTATION": "Diagnostics should appear in IntelliJ; the server capability tweak alone did not fix it and client-side handling must be investigated.",
    "NEW INSTRUCTION": "WHEN diagnostics persist after server capability change THEN trace publishDiagnostics and inspect client handlers and selectors"
}

