#!/bin/sh
# End-to-end API smoke of BOS-010 F2 through a locally running gateway (see docs/LOCAL_DEVELOPMENT.md):
# applicant draft -> document -> submit -> review -> duplicates/note -> fee payment -> approve -> activate ->
# suspend -> reactivate. Uses the local/test development OTP code; creates smoke data in the local databases.
# Nonzero exit on the first unexpected response.
set -eu

GATEWAY="${GATEWAY_URL:-http://localhost:8080}"
ADMIN_PHONE="${SMOKE_ADMIN_PHONE:-01306999005}"
APPLICANT_PHONE="${SMOKE_APPLICANT_PHONE:-01711000001}"
DEV_OTP_CODE="000000"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

fail() { echo "SMOKE FAILED: $1" >&2; exit 1; }

case "$GATEWAY" in
    http://localhost:*|http://127.0.0.1:*) ;;
    *) fail "GATEWAY_URL must be a local gateway; this script uses the development OTP code" ;;
esac
command -v curl >/dev/null 2>&1 || fail "curl is required"
command -v python3 >/dev/null 2>&1 || fail "python3 is required"

# call METHOD PATH TOKEN EXPECTED_STATUS [JSON_BODY] -> response body on stdout
call() {
    method="$1"; path="$2"; token="$3"; expected="$4"; body="${5:-}"
    set -- -sS -o "$WORK/body" -w '%{http_code}' -X "$method" -H "Content-Type: application/json"
    [ -n "$token" ] && set -- "$@" -H "Authorization: Bearer $token"
    [ -n "$body" ] && set -- "$@" --data "$body"
    status=$(curl "$@" "$GATEWAY$path") || fail "$method $path: gateway unreachable"
    [ "$status" = "$expected" ] || fail "$method $path: expected $expected, got $status: $(cat "$WORK/body")"
    cat "$WORK/body"
}

# field JSON_ON_STDIN DOTTED.PATH
field() {
    python3 -c 'import json,sys
v=json.load(sys.stdin)
for k in sys.argv[1].split("."):
    v=v[int(k)] if k.isdigit() else v.get(k) if isinstance(v, dict) else None
print("" if v is None else v)' "$1"
}

login() {
    attempt=$(call POST /api/v1/auth/otp/start "" 200 "{\"phone\":\"$1\"}" | field data.attemptId)
    call POST /api/v1/auth/otp/verify "" 200 \
        "{\"attemptId\":\"$attempt\",\"phone\":\"$1\",\"code\":\"$DEV_OTP_CODE\"}" | field data.accessToken
}

echo "==> Logging in (development OTP): admin $ADMIN_PHONE, applicant $APPLICANT_PHONE"
ADMIN=$(login "$ADMIN_PHONE")
APPLICANT=$(login "$APPLICANT_PHONE")

echo "==> Creation fee schedule"
schedule_status=$(curl -sS -o "$WORK/fee" -w '%{http_code}' -H "Authorization: Bearer $ADMIN" \
    "$GATEWAY/api/v1/platform/fees/BUILDING_CREATION")
if [ "$schedule_status" != "200" ]; then
    [ "${SMOKE_SET_FEE:-0}" = "1" ] || fail "BUILDING_CREATION fee is not configured (HTTP $schedule_status). \
Configure it (PUT /api/v1/platform/fees/BUILDING_CREATION) or re-run with SMOKE_SET_FEE=1 to set a local test value."
    call PUT /api/v1/platform/fees/BUILDING_CREATION "$ADMIN" 200 \
        '{"amount":1000.00,"currency":"BDT","required":true}' > "$WORK/fee"
    echo "    set local test schedule 1000.00 BDT (SMOKE_SET_FEE=1)"
fi
FEE_AMOUNT=$(field data.amount < "$WORK/fee")
FEE_CURRENCY=$(field data.currency < "$WORK/fee")
FEE_REQUIRED=$(field data.required < "$WORK/fee")

echo "==> Applicant: draft, document, submit"
SUFFIX=$(date +%s)
APP=$(call POST /api/v1/building-applications "$APPLICANT" 201 "{\"buildingName\":\"Smoke Tower $SUFFIX\",
  \"buildingType\":\"RESIDENTIAL\",\"address\":\"House $SUFFIX, Road 1\",\"area\":\"Mirpur\",\"district\":\"Dhaka\",
  \"estimatedUnits\":12,\"applicantRelationship\":\"OWNER\",\"contactName\":\"Smoke Applicant\",
  \"contactPhone\":\"$APPLICANT_PHONE\",\"latitude\":23.8069,\"longitude\":90.3687}" | field data.id)
printf '%%PDF-1.4\n%% smoke\n' > "$WORK/deed.pdf"
doc_status=$(curl -sS -o "$WORK/body" -w '%{http_code}' -H "Authorization: Bearer $APPLICANT" \
    -F "file=@$WORK/deed.pdf;type=application/pdf" "$GATEWAY/api/v1/building-applications/$APP/documents")
[ "$doc_status" = "201" ] || fail "document upload: expected 201, got $doc_status: $(cat "$WORK/body")"
DOC=$(field data.id < "$WORK/body")
curl -sS -f -o "$WORK/download" -H "Authorization: Bearer $ADMIN" \
    "$GATEWAY/api/v1/building-applications/$APP/documents/$DOC" || fail "document download failed"
cmp -s "$WORK/deed.pdf" "$WORK/download" || fail "downloaded document differs from upload"
call POST "/api/v1/building-applications/$APP/submit" "$APPLICANT" 200 > /dev/null

echo "==> Admin: review, duplicates, note"
call POST "/api/v1/platform/building-applications/$APP/start-review" "$ADMIN" 200 > /dev/null
call GET "/api/v1/platform/building-applications/$APP/duplicates" "$ADMIN" 200 > /dev/null
call POST "/api/v1/platform/building-applications/$APP/notes" "$ADMIN" 201 '{"body":"Smoke test note"}' > /dev/null
call GET "/api/v1/building-applications/$APP" "$APPLICANT" 200 | grep -q "Smoke test note" \
    && fail "internal note leaked to the applicant"

if [ "$FEE_REQUIRED" = "True" ] || [ "$FEE_REQUIRED" = "true" ]; then
    echo "==> Approval blocked until the fee is recorded, then record $FEE_AMOUNT $FEE_CURRENCY"
    call POST "/api/v1/platform/building-applications/$APP/approve" "$ADMIN" 409 \
        "{\"adminPhone\":\"$APPLICANT_PHONE\",\"reason\":\"Smoke approval\"}" | grep -q CREATION_FEE_UNPAID \
        || fail "approval was not blocked by the unpaid fee"
    call POST /api/v1/platform/fees/BUILDING_CREATION/payments "$ADMIN" 201 "{\"referenceType\":\"BUILDING_APPLICATION\",
      \"referenceId\":\"$APP\",\"amount\":$FEE_AMOUNT,\"currency\":\"$FEE_CURRENCY\",\"externalReference\":\"SMOKE-$SUFFIX\",
      \"paidOn\":\"$(date +%Y-%m-%d)\"}" > /dev/null
fi

echo "==> Approve, activate, suspend, reactivate"
BUILDING=$(call POST "/api/v1/platform/building-applications/$APP/approve" "$ADMIN" 200 \
    "{\"adminPhone\":\"$APPLICANT_PHONE\",\"reason\":\"Smoke approval\"}" | field data.buildingId)
for step in activate:ACTIVE suspend:SUSPENDED reactivate:ACTIVE; do
    action=${step%%:*}; want=${step#*:}
    got=$(call POST "/api/v1/platform/buildings/$BUILDING/$action" "$ADMIN" 200 "{\"reason\":\"Smoke $action\"}" \
        | field data.status)
    [ "$got" = "$want" ] || fail "$action: expected $want, got $got"
done
transitions=$(call GET "/api/v1/building-applications/$APP/history" "$APPLICANT" 200 \
    | python3 -c 'import json,sys; print(",".join(t["toStatus"] for t in json.load(sys.stdin)["data"]))')
[ "$transitions" = "DRAFT,SUBMITTED,UNDER_REVIEW,APPROVED" ] || fail "unexpected application history: $transitions"

echo "SMOKE PASSED: application $APP -> building $BUILDING (ACTIVE, admin $APPLICANT_PHONE)"
