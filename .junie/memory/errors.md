[2026-01-06 14:09] - Updated by Junie - Error analysis
{
    "TYPE": "invalid args",
    "TOOL": "search_replace",
    "ERROR": "Missing or malformed arguments for search_replace",
    "ROOT CAUSE": "The search_replace invocation omitted required parameters, so the tool could not execute.",
    "PROJECT NOTE": "Edit gradle.properties and src/main/resources/META-INF/plugin.xml for naming; confirm contents before replacements.",
    "NEW INSTRUCTION": "WHEN invoking search_replace tool THEN provide file_path, search text, and replacement text explicitly"
}

[2026-01-06 14:52] - Updated by Junie - Error analysis
{
    "TYPE": "env/setup",
    "TOOL": "knipLanguageServer (npx)",
    "ERROR": "npx failed: could not determine executable to run",
    "ROOT CAUSE": "The server launch used npx without a resolvable @knip/language-server executable or incorrect npx path.",
    "PROJECT NOTE": "Add a preflight in KnipSettings/StreamProvider to verify Node.js and npx paths and run `npx @knip/language-server --version` before startup; surface a clear notification on failure.",
    "NEW INSTRUCTION": "WHEN npx stderr contains \"could not determine executable to run\" THEN start server via Node.js using resolved package bin path"
}

