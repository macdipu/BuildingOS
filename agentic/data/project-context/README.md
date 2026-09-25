# Project-context layout

```text
agentic/data/project-context/
├── project.yaml          # project identity, repos, modules, integrations
├── context-index.yaml    # freshness/status index for modules and features
├── kit-runtime.json      # kit's own module-context snapshot
├── BRD.md                # whole-app/whole-project BRD, optional
├── PRD.md                # whole-app/whole-project PRD, optional
├── SRD.md                # whole-app/whole-project SRD/SRS, optional
├── ARCHITECTURE.md       # project-level HLD, generated + human-approved (template: project-hld.md)
├── ui/<set>/             # UI mockups (screen.png + code.html per screen), design tokens, UI-INDEX.md
└── features/<WORK-ITEM-ID>/   # per-feature/CR/bug artifacts, see features/README.md
```

## Project-wide vs feature-scoped docs

- `BRD.md`, `PRD.md`, `SRD.md` at this root describe the whole application/project —
  cross-feature business objectives, product scope, and system-wide requirements that
  outlive any single work item.
- A feature/CR/bug's own `BRD.md`/`SRS.md` under `features/<WORK-ITEM-ID>/` scope those
  same concerns to that work item, and should reference the project-wide doc (via
  `## References`) rather than restate it.
- Templates: `agentic/kit/templates/brd.md`, `srs.md` (used for both PRD- and SRD-shaped
  content; the kit does not distinguish a separate PRD template), `project-hld.md` (root
  `ARCHITECTURE.md`, project-level HLD), `feature-hld.md` (feature `ARCHITECTURE.md`,
  feature-scoped HLD), `feature-lld.md` (feature `TECH-SPEC.md`, LLD).
- Same rule as feature docs: only create the project-wide file when the project actually
  has one — do not create empty placeholders. If the project has no whole-app BRD/PRD/SRD,
  leave these absent.
- When a project-wide doc is added, removed, or changes materially, update
  `context-index.yaml`'s `project_docs` map (path + status), mirroring how `features` are
  tracked.

### HLD / LLD aliases

| Doc | Alias | Scope | Template |
| --- | --- | --- | --- |
| `ARCHITECTURE.md` (root) | HLD | whole project | `project-hld.md` |
| `features/<ID>/ARCHITECTURE.md` | HLD | one work item | `feature-hld.md` |
| `features/<ID>/TECH-SPEC.md` | LLD | one work item | `feature-lld.md` |

Filenames stay as-is; "HLD"/"LLD" are aliases, not separate files.

- When a work item reaches TECHNICAL stage and root `ARCHITECTURE.md` does not exist
  yet, `technical-architecture-planner` drafts it from `project-hld.md` at `status: DRAFT`.
- A drafted root HLD is not a baseline. Per AGENTS.md #10, no skill may mark it
  `APPROVED`; a human approves by recording `approved_by`/`approved_at` on its
  `context-index.yaml` `project_docs.ARCHITECTURE` entry after review.
