#! /bin/bash
set -x
# Usage: ./uploadArtifact.sh path/to/file https://holywen.jfrog.io REPO_NAME path/to/artifact USER:TOKEN

export FILE_PATH=${1:-"path/to/file"}
export ARTIFACTORY_URL=${2:-"https://holywen.jfrog.io"}
export REPO_NAME=${3:-"REPO_NAME"}
export ARTIFACT_PATH=${4:-"path/to/artifact"}
export API_TOKEN=${5:-"USER:TOKEN"}

curl -H "Authorization: Bearer ${API_TOKEN}" -T ${FILE_PATH} "${ARTIFACTORY_URL}/artifactory/${REPO_NAME}/${ARTIFACT_PATH}"
