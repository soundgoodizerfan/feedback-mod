#!/usr/bin/env bash
# Build the mod, copy the jar into the feedback-test-pack packwiz repo, and refresh its index.
# NeoForge 1.21.1 wants JDK 21, which is the machine default -- no JAVA_HOME override needed.
set -euo pipefail

cd "$(dirname "$0")"

pack_dir="/home/colby/Documents/GitHub/feedback-test-pack"
instance_dest="$pack_dir/mods"

if [ ! -f "$pack_dir/pack.toml" ]; then
    echo "packwiz repo not found: $pack_dir" >&2
    exit 1
fi
mkdir -p "$instance_dest"

./gradlew build --no-daemon

mod_id=$(grep '^mod_id=' gradle.properties | cut -d= -f2)
mod_version=$(grep '^mod_version=' gradle.properties | cut -d= -f2)
jar="build/libs/${mod_id}-${mod_version}.jar"

if [ ! -f "$jar" ]; then
    echo "Expected jar not found: $jar" >&2
    echo "Contents of build/libs:" >&2
    ls -1 build/libs >&2 || true
    exit 1
fi

# A version bump leaves the previous jar behind, and two copies of the same mod id
# is a hard crash on launch. Clear our own jars only -- never anything else in mods/.
shopt -s nullglob
for stale in "$instance_dest/${mod_id}-"*.jar; do
    echo "Removing stale $(basename "$stale")"
    rm -f "$stale"
done
shopt -u nullglob

cp "$jar" "$instance_dest/"
echo "Deployed $(basename "$jar") -> $instance_dest"

# Runtime deps that are not jarJar'd into our jar: Sable and Curios (required), JEI and Jade (dev-only, but
# wanted for testing). Flywheel is embedded; Ponder is skipped, it would drag in its own Flywheel.
# Pinned to the versions we compile against; re-adding overwrites the old pin.
prop() { grep "^$1=" gradle.properties | cut -d= -f2; }
mc=$(prop minecraft_version)
# --version-filename only searches a project's newest versions, so JEI silently resolved to latest.
# Look the version id up by filename ourselves.
mr_add() {
    local id
    id=$(curl -fsS -A feedback-build "https://api.modrinth.com/v2/project/$1/version" |
        python3 -c 'import json,sys; print(next(v["id"] for v in json.load(sys.stdin) if any(f["filename"]==sys.argv[1] for f in v["files"])))' "$2")
    (cd "$pack_dir" && packwiz mr add --project-id "$1" --version-id "$id" -y)
}
mr_add T9PomCSv "sable-neoforge-${mc}-$(prop sable_version).jar"
mr_add u6dRKJwZ "jei-${mc}-neoforge-$(prop jei_version).jar"
mr_add vvuO3ImH "curios-neoforge-$(prop curios_version).jar"
mr_add nvQzSEkH "Jade-${mc}-NeoForge-$(prop jade_version | sed 's/+neoforge$//').jar"
# Better Combat and its Cloth Config: required by Feedback (UpswingSwing, SwingClips); pinned like Sable and Curios.
mr_add 5sy6g3kz "bettercombat-neoforge-$(prop better_combat_version | sed 's/-neoforge$//').jar"
mr_add 9s6osm5g "cloth-config-$(prop cloth_config_version | sed 's/+neoforge$//')-neoforge.jar"

(cd "$pack_dir" && packwiz refresh)
