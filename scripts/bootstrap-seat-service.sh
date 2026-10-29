#!/usr/bin/env bash
# Run once from the kippu repo root, after unzipping the M2 files over the repo.
# The M2 zip contains only code and config. This script gives seat-service the same
# build files as catalog-service (pom.xml, Maven wrapper) and registers the module.
set -euo pipefail

SRC=services/catalog-service
DST=services/seat-service

[ -f "$SRC/pom.xml" ] || { echo "Run this from the kippu repo root (could not find $SRC/pom.xml)"; exit 1; }
[ -d "$DST/src" ]     || { echo "Could not find $DST/src. Did you unzip the M2 files here?"; exit 1; }

if [ -f "$DST/pom.xml" ]; then
  echo "$DST/pom.xml already exists, leaving it alone."
else
  cp "$SRC/pom.xml" "$DST/pom.xml"
  cp "$SRC/mvnw" "$SRC/mvnw.cmd" "$DST/"
  cp -R "$SRC/.mvn" "$DST/.mvn"
  chmod +x "$DST/mvnw"
  sed -i.bak 's/catalog-service/seat-service/g' "$DST/pom.xml" && rm "$DST/pom.xml.bak"
  echo "Created $DST/pom.xml from catalog-service. Lines that still mention 'catalog':"
  grep -n -i catalog "$DST/pom.xml" || echo "  (none)"
fi

if grep -q 'services/seat-service' pom.xml; then
  echo "Root pom.xml already lists seat-service."
else
  perl -0pi -e 's#(\s*)<module>services/catalog-service</module>#$&$1<module>services/seat-service</module>#' pom.xml
  grep -q 'services/seat-service' pom.xml \
    && echo "Added <module>services/seat-service</module> to the root pom.xml." \
    || echo "Could not edit the root pom.xml automatically. Add <module>services/seat-service</module> by hand."
fi
