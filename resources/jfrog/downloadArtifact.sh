#! /bin/bash
set -x
# Usage: ./downloadArtifact.sh https://holywen.jfrog.io REPO_NAME path/to/artifact TOKEN

export ARTIFACTORY_URL=${1:-"https://holywen.jfrog.io"}
export REPO_NAME=${2:-"REPO_NAME"}
export ARTIFACT_PATH=${3:-"path/to/artifact"}
export API_TOKEN=${4:-"TOKEN"}

curl -H "Authorization: Bearer ${API_TOKEN}" -L -O  "${ARTIFACTORY_URL}/artifactory/${REPO_NAME}/${ARTIFACT_PATH}"
