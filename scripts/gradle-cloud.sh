#!/usr/bin/env bash
# Java does not automatically consume HTTP(S)_PROXY. Preserve the platform route.
set -euo pipefail
cd "$(dirname "$0")/.."
proxy_args=()
proxy_url="${HTTPS_PROXY:-${https_proxy:-}}"
if [[ -n "$proxy_url" ]]; then
  proxy_flags=$(python3 - <<'PY'
import os, re
from urllib.parse import urlsplit
proxy = urlsplit(os.environ.get('HTTPS_PROXY') or os.environ.get('https_proxy', ''))
host = proxy.hostname or ''
if proxy.scheme != 'http' or not re.fullmatch(r'[A-Za-z0-9.-]+', host):
    raise SystemExit('Unsupported proxy format; configure Java proxy settings explicitly.')
port = proxy.port or 80
print(f'-Dhttps.proxyHost={host} -Dhttps.proxyPort={port} -Dhttp.proxyHost={host} -Dhttp.proxyPort={port}')
PY
)
  read -r -a proxy_args <<< "$proxy_flags"
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} $proxy_flags"
fi
# The session CA is in the system Java trust store; downloaded JDKs lack it.
if [[ -f /etc/ssl/certs/java/cacerts ]]; then
  proxy_args+=("-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts")
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
fi
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PWD/.cache/gradle}"
exec ./gradlew "${proxy_args[@]}" "$@"
