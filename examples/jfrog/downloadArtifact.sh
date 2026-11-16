#! /bin/bash
set -euo pipefail
# Usage: ./downloadArtifact.sh https://holywen.jfrog.io REPO_NAME path/to/artifact   (API_TOKEN from env)

export ARTIFACTORY_URL=${1:-"https://holywen.jfrog.io"}
export REPO_NAME=${2:-"REPO_NAME"}
export ARTIFACT_PATH=${3:-"path/to/artifact"}
: "${API_TOKEN:?export API_TOKEN in the environment; do not pass tokens as arguments}"
export API_TOKEN

curl -H "Authorization: Bearer ${API_TOKEN}" -L -O  "${ARTIFACTORY_URL}/artifactory/${REPO_NAME}/${ARTIFACT_PATH}"
