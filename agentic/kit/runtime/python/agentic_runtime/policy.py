from dataclasses import dataclass

WORK_TYPES = {"new_feature", "change_request", "bug", "hotfix", "technical_change", "security_change", "discovery", "existing_task", "device_preview"}
PLANNING = {"FULL_SPRINT_PLANNING", "ADD_TO_EXISTING_SPRINT", "BACKLOG_ONLY", "EXPEDITED", "NO_REPLAN"}
STAGES = {"INTAKE", "CONTEXT", "REQUIREMENTS", "IMPACT", "TECHNICAL", "PLANNING", "IMPLEMENTATION", "PREVIEW", "REVIEW", "QA", "UAT", "RELEASE", "COMPLETED"}
REQUIRED_GATES = {"IMPLEMENTATION": "technical", "RELEASE": "release"}


@dataclass
class PolicyDecision:
    allowed: bool
    reasons: list


def workflow_route(work_type, planning="NO_REPLAN", require_uat=False):
    if work_type not in WORK_TYPES or planning not in PLANNING:
        raise ValueError("Unknown work type or sprint handling")
    route = ["INTAKE", "CONTEXT"]
    if work_type == "device_preview":
        return route + ["PREVIEW", "COMPLETED"]
    if work_type == "discovery":
        return route + ["REVIEW", "COMPLETED"]
    if work_type == "new_feature":
        route += ["REQUIREMENTS", "TECHNICAL"]
    elif work_type != "existing_task":
        route += ["IMPACT", "TECHNICAL"]
    if planning in {"FULL_SPRINT_PLANNING", "ADD_TO_EXISTING_SPRINT", "BACKLOG_ONLY"}:
        route += ["PLANNING"]
    if planning == "BACKLOG_ONLY":
        return route + ["COMPLETED"]
    return route + ["IMPLEMENTATION", "REVIEW", "QA"] + (["UAT"] if require_uat else []) + ["RELEASE", "COMPLETED"]


def evaluate(stage, approvals):
    if stage not in STAGES:
        return PolicyDecision(False, ["Unknown or unsupported stage: " + stage])
    gate = REQUIRED_GATES.get(stage)
    reasons = ["Missing required approval gate: " + gate] if gate and not approvals.get(gate, False) else []
    return PolicyDecision(not reasons, reasons)


# Unattended approval is only ever wired to the 'technical' gate (see
# Orchestrator.approve_auto); 'release' and 'uat' always require a real human,
# no matter how this set changes.
AUTO_APPROVAL_WORK_TYPES = {"bug", "hotfix"}


def auto_approval_eligible(work_type, context_files, technical_result):
    """Conservative, evidence-gated check for an unattended technical-gate approval.

    Every condition must hold; missing or ambiguous evidence fails closed. The
    verdict field checked here (outputs.verdict == TECHNICAL_READY) is the same
    field technical-readiness-verifier already emits for human reviewers -- this
    does not invent a new trust signal, it just acts on the existing one when the
    blast radius is small enough (single reviewed file, bug/hotfix only).
    """
    if work_type not in AUTO_APPROVAL_WORK_TYPES:
        return False, "auto-approval only covers bug/hotfix work items"
    if not context_files or len(context_files) > 1:
        return False, "auto-approval requires a single reviewed scope file"
    if not isinstance(technical_result, dict) or technical_result.get("status") != "READY":
        return False, "technical stage evidence is not READY"
    outputs = technical_result.get("outputs")
    if not isinstance(outputs, dict) or outputs.get("verdict") != "TECHNICAL_READY":
        return False, "technical-readiness-verifier did not attest TECHNICAL_READY"
    return True, ""
