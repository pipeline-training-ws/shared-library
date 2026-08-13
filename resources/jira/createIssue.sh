#! /bin/bash
set -x

JIRA_KEY=${1:-"SCRUM"}
JIRA_ISSUE_TYPE=${2:-"Task"}
JIRA_DESCRIPTION=${3:-"MY ISSUE DESCRIPTION"}
JIRA_SUMMARY=${4:-"MY ISSUE SUMMARY"}
JIRA_URL=${5:-"https://jira.atlassian.net/"}
JIRA_TOKEN=${6:-"USER:TOKEN"}
JIRA_ASSIGNEE=${7:-}

# Build the JSON with jq --arg instead of a heredoc so JIRA_SUMMARY/JIRA_DESCRIPTION
# (untrusted text) can never be re-parsed as shell syntax (e.g. command substitution).
jq -n \
  --arg key "$JIRA_KEY" \
  --arg summary "$JIRA_SUMMARY" \
  --arg description "$JIRA_DESCRIPTION" \
  --arg issuetype "$JIRA_ISSUE_TYPE" \
  --arg assignee "$JIRA_ASSIGNEE" \
  '{fields: ({project: {key: $key}, summary: $summary, description: $description, issuetype: {name: $issuetype}}
             + (if $assignee != "" then {assignee: {name: $assignee}} else {} end))}' \
  > createIssue.json

cat createIssue.json
curl -D- -u "$JIRA_TOKEN" -X POST --data @createIssue.json -H "Content-Type: application/json" "$JIRA_URL/rest/api/2/issue"
