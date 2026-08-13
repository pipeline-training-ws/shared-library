def call(Map config=[:]) {
    withCredentials([usernamePassword(credentialsId: 'jira-user-token', passwordVariable: 'JIRA_PW', usernameVariable: 'JIRA_USER')]) {

        sh """
cat <<EOF > createIssue.json
{
   "fields": {
      "project": {
         "key": "${config.JIRA_KEY}"
      },
      "summary": "${config.JIRA_SUMMARY}",
      "description": "${config.JIRA_DESCRIPTION}",
      "issuetype": {
         "name": "${config.JIRA_ISSUE_TYPE}"
      },
      "assignee": {
         "name": "assignee-username"
      }
   }
}
EOF
        """


        //def jiraToken="${JIRA_USER}:${JIRA_PW}"
        sh """
        cat createIssue.json
        curl -D- -o createIssueResult.json -u "${JIRA_USER}:${JIRA_PW}" -X POST --data @createIssue.json -H "Content-Type: application/json" ${config.JIRA_URL}/rest/api/2/issue
        """
        archiveArtifacts artifacts: '*.*', followSymlinks: false
    }

}