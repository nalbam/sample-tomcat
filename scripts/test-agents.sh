#!/bin/bash
# Checks that scripts/agents.sh removes only the containers that it created, without Docker.
# A stub docker logs its arguments; "docker run" fails, with or without creating the container.
# Usage: scripts/test-agents.sh
AGENTS="$(cd "$(dirname "$0")" && pwd)/agents.sh"

STUB="$(mktemp -d)"
trap 'rm -rf "${STUB}"' EXIT
mkdir -p "${STUB}/bin"
cat > "${STUB}/bin/docker" <<'STUB_EOF'
#!/bin/bash
echo "$*" >> "${DOCKER_LOG}"
if [ "$1" = "run" ]; then
  # STUB_CREATE=1: the container is created, then the start fails (for example, a busy port).
  if [ "${STUB_CREATE}" = "1" ]; then
    while [ $# -gt 0 ]; do
      [ "$1" = "--cidfile" ] && echo "created-by-run" > "$2"
      shift
    done
  fi
  exit 1
fi
exit 0
STUB_EOF
chmod +x "${STUB}/bin/docker"

FAILED=0

# check <name> <STUB_CREATE> <rm arguments that must be the only rm calls, or "">
check() {
  local name="$1" log="${STUB}/$1.log"
  : > "${log}"
  if PATH="${STUB}/bin:${PATH}" DOCKER_LOG="${log}" STUB_CREATE="$2" "${AGENTS}" stub-image > /dev/null 2>&1; then
    echo "FAIL ${name}: agents.sh succeeded although docker run failed"
    FAILED=1
    return
  fi
  local rms
  rms="$(grep -E '^rm( |$)' "${log}" | xargs)"
  if [ "${rms}" != "$3" ]; then
    echo "FAIL ${name}: want rm calls '$3', got '${rms}'"
    FAILED=1
  elif grep -qE '^run .*--name agents ' "${log}"; then
    echo "FAIL ${name}: uses the shared container name 'agents'"
    FAILED=1
  else
    echo "ok   ${name}"
  fi
}

# A container named "agents" that belongs to the user is never removed.
check "run fails before create" 0 ""
check "run creates, start fails" 1 "rm -f created-by-run"

exit ${FAILED}
