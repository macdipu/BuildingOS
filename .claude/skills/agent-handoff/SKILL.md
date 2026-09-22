---
name: agent-handoff
description: Read and write this project's cross-agent-platform handoff notes (/agent-handoff)
---

Before starting work, read `.agent/HANDOFF.md` and the most recent file under
`.agent/sessions/` (or run `python3 agentic/kit/runtime/python/agentic_runtime/cli.py pickup`)
to pick up where the last agent -- on this platform or another -- left off.

When you finish a work session, record it so a different agent/platform can
continue from git alone:

```
python3 agentic/kit/runtime/python/agentic_runtime/cli.py close-session \
  --agent claude --status RUNNING|BLOCKED|COMPLETED|CANCELLED \
  --task "what this run is for" --completed "what you did" \
  --changed-files path/one path/two --tests "npm test passes" \
  --blockers "..." --decisions "..." --next-action "what to do next"
```

`--changed-files`, `--tests`, `--blockers`, and `--decisions` are optional;
`--agent`, `--status`, `--task`, and `--completed` are not -- `--status` has no
default, so it can't silently claim `COMPLETED` for a session that didn't
finish. Structured fields, not one free-form summary, so the next agent can
read a specific field instead of parsing prose.

This writes `.agent/HANDOFF.md` and a new `.agent/sessions/<timestamp>-claude.md`
record, both git-tracked (unlike this kit's own local run store under
`agentic/data/runtime/state/`, which stays out of git). Never overwrite another
agent's uncommitted changes without explicit user approval.
