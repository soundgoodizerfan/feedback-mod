#!/usr/bin/env bash
# Lists faces of the procedural gun model that lie in one plane, face the same way and overlap (they z-fight).
# Runs client/gun/GunFaceCheck offline over a sweep of synthetic guns. Exits 1 when any are found.
# Compiles first; takes the runtime classpath from Gradle.
set -euo pipefail
cd "$(dirname "$0")/.."
init=$(mktemp --suffix=.gradle)
trap 'rm -f "$init"' EXIT
cat > "$init" <<'G'
allprojects {
    tasks.register('printGunCheckClasspath') {
        dependsOn 'classes'
        def cp = project.sourceSets.main.runtimeClasspath
        doLast { println 'CP=' + cp.asPath }
    }
}
G
cp=$(./gradlew -q --init-script "$init" printGunCheckClasspath | sed -n 's/^CP=//p')
# Run from a scratch directory: Minecraft's logger writes logs/ into the working directory.
cd "$(mktemp -d)"
exec java -cp "$cp" io.github.soundgoodizerfan.feedback.client.gun.GunFaceCheck
