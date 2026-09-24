#!/usr/bin/env python3
"""Validate the platform contracts (OpenAPI + Kafka envelope and event schemas). Nonzero exit on any problem."""
import json
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parent.parent
OPENAPI_PATH = ROOT / "contracts" / "openapi" / "platform.yaml"
ENVELOPE_SCHEMA_PATH = ROOT / "contracts" / "kafka" / "event-envelope.schema.json"

errors = []


def check_openapi():
    if not OPENAPI_PATH.is_file():
        errors.append(f"missing {OPENAPI_PATH}")
        return
    try:
        spec = yaml.safe_load(OPENAPI_PATH.read_text())
    except yaml.YAMLError as exc:
        errors.append(f"{OPENAPI_PATH}: invalid YAML: {exc}")
        return
    if not isinstance(spec, dict):
        errors.append(f"{OPENAPI_PATH}: root must be a mapping")
        return
    if not str(spec.get("openapi", "")).startswith("3."):
        errors.append(f"{OPENAPI_PATH}: missing/unsupported 'openapi' version field")
    paths = spec.get("paths")
    if not isinstance(paths, dict) or not paths:
        errors.append(f"{OPENAPI_PATH}: no paths defined")
        return
    for path, operations in paths.items():
        if not isinstance(operations, dict):
            errors.append(f"{OPENAPI_PATH}: path {path} has no operations")
            continue
        for method, operation in operations.items():
            responses = operation.get("responses", {}) if isinstance(operation, dict) else {}
            if not responses:
                errors.append(f"{OPENAPI_PATH}: {method.upper()} {path} declares no responses")
            for status in ("401",):
                if status not in responses:
                    errors.append(f"{OPENAPI_PATH}: {method.upper()} {path} missing {status} response")


def check_kafka_envelope_schema():
    if not ENVELOPE_SCHEMA_PATH.is_file():
        errors.append(f"missing {ENVELOPE_SCHEMA_PATH}")
        return
    try:
        schema = json.loads(ENVELOPE_SCHEMA_PATH.read_text())
    except json.JSONDecodeError as exc:
        errors.append(f"{ENVELOPE_SCHEMA_PATH}: invalid JSON: {exc}")
        return
    if schema.get("type") != "object":
        errors.append(f"{ENVELOPE_SCHEMA_PATH}: root schema type must be 'object'")
    required = set(schema.get("required", []))
    # SRS-BOS-001 PF-03: eventId, eventVersion, occurredAt, producer, correlationId (+ buildingId when applicable).
    expected_required = {"eventId", "eventType", "eventVersion", "occurredAt", "producer", "correlationId", "data"}
    missing = expected_required - required
    if missing:
        errors.append(f"{ENVELOPE_SCHEMA_PATH}: required fields missing: {sorted(missing)}")
    properties = schema.get("properties", {})
    for field in expected_required | {"buildingId"}:
        if field not in properties:
            errors.append(f"{ENVELOPE_SCHEMA_PATH}: properties missing '{field}'")


def check_kafka_event_schemas():
    """Each <event-type>.v<N>.schema.json describes the envelope `data` of one published event version."""
    schemas = sorted(ENVELOPE_SCHEMA_PATH.parent.glob("*.v*.schema.json"))
    if not schemas:
        errors.append(f"{ENVELOPE_SCHEMA_PATH.parent}: no event data schemas")
    for path in schemas:
        try:
            schema = json.loads(path.read_text())
        except json.JSONDecodeError as exc:
            errors.append(f"{path}: invalid JSON: {exc}")
            continue
        if schema.get("type") != "object" or schema.get("additionalProperties") is not False:
            errors.append(f"{path}: data schema must be a closed object (additionalProperties: false)")
        missing = set(schema.get("required", [])) - set(schema.get("properties", {}))
        if not schema.get("required") or missing:
            errors.append(f"{path}: required fields must be declared properties (missing: {sorted(missing)})")


def main():
    check_openapi()
    check_kafka_envelope_schema()
    check_kafka_event_schemas()
    if errors:
        for error in errors:
            print(f"CONTRACT CHECK FAILED: {error}", file=sys.stderr)
        return 1
    print("CONTRACT CHECKS PASSED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
