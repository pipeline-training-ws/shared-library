#! /bin/bash
set -x

export JIRA_KEY=${1:-"SCRUM"}
export JIRA_ISSUE_TYPE=${2:-"Task"}
export JIRA_DESCRIPTION=${3:-"MY ISSUE DESCRIPTION"}
export JIRA_SUMMARY=${4:-"MY ISSUE SUMMARY"}
export JIRA_URL=${5:-"https://jira.atlassian.net/"}
export JIRA_TOKEN=${6:-"USER:TOKEN"}



cat <<EOF> createIssue.json
{
   "fields": {
      "project": {
         "key": "${JIRA_KEY}"
      },
      "summary": "${JIRA_SUMMARY}",
      "description": "${JIRA_DESCRIPTION}",
      "issuetype": {
         "name": "${JIRA_ISSUE_TYPE}"
      },
      "assignee": {
         "name": "assignee-username"
      }
   }
}
EOF

cat createIssue.json
curl -D- -u $JIRA_TOKEN -X POST --data @createIssue.json -H "Content-Type: application/json" $JIRA_URL/rest/api/2/issue
