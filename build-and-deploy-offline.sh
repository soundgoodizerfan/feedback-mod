#!/usr/bin/env bash
# Same as build-and-deploy.sh, but Gradle runs --offline (cached deps only). Use when a maven host is down or its cert is bad.
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

# Datagen first so the jar carries fresh src/generated/resources.
./gradlew runData --no-daemon --offline
./gradlew build --no-daemon --offline

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

deployed="${mod_id}-$(date +%Y%m%d).jar"
cp "$jar" "$instance_dest/$deployed"
echo "Deployed $deployed -> $instance_dest"

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

# Push the mod repo's existing commits first (never commits for you; no message needed).
gh_push() { git -c credential.helper= -c credential.helper='!gh auth git-credential' push origin HEAD; }
mod_head=$(git rev-parse HEAD)
if [ -n "$(git status --porcelain)" ]; then
    echo "WARNING: uncommitted changes are in this jar but not pushed; only committed work goes to the remote" >&2
fi
gh_push

# Pack commit body: mod commit subjects since the last deploy, found via the Feedback-Commit trailer.
# First run (no trailer yet) lists the last 20 commits.
last=$(git -C "$pack_dir" log -1 --format=%B | sed -n 's/^Feedback-Commit: //p')
if [ -n "$last" ] && git cat-file -e "$last^{commit}" 2>/dev/null; then
    range="$last..HEAD"
else
    range="-20"
fi
changes=$(git log --format='- %s' $range)

# Commit the pack and push it through gh's credential helper (plain git has no credentials here).
# gh itself cannot commit; git does, gh only supplies auth.
(
    cd "$pack_dir"
    git add -A
    if git diff --cached --quiet; then
        echo "Pack unchanged, nothing to commit"
    else
        git commit -m "Deploy ${mod_id} $(date +%Y%m%d)" -m "${changes:-No new mod commits}" -m "Feedback-Commit: $mod_head"
        git -c credential.helper= -c credential.helper='!gh auth git-credential' push origin HEAD
    fi
)
