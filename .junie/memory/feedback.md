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

