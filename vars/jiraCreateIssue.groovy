def call(Map config=[:]) {
    withCredentials([usernamePassword(credentialsId: 'jira-user-token', passwordVariable: 'JIRA_PW', usernameVariable: 'JIRA_USER')]) {

        def fields = [
            project    : [key: config.JIRA_KEY],
            summary    : config.JIRA_SUMMARY,
            description: config.JIRA_DESCRIPTION,
            issuetype  : [name: config.JIRA_ISSUE_TYPE]
        ]
        if (config.JIRA_ASSIGNEE) {
            fields.assignee = [name: config.JIRA_ASSIGNEE]
        }
        // Serialize with writeJSON instead of a shell heredoc: JIRA_SUMMARY/JIRA_DESCRIPTION
        // are untrusted text and must never be re-parsed by the shell.
        writeJSON file: 'createIssue.json', json: [fields: fields]

        withEnv(["JIRA_URL=${config.JIRA_URL}"]) {
            // Single-quoted sh: values are resolved by the shell from env, not
            // interpolated into the script text, so they can't be re-parsed as shell syntax.
            sh '''
                cat createIssue.json
                curl -D- -o createIssueResult.json -u "$JIRA_USER:$JIRA_PW" -X POST --data @createIssue.json -H "Content-Type: application/json" "$JIRA_URL/rest/api/2/issue"
            '''
        }
        archiveArtifacts artifacts: '*.*', followSymlinks: false
    }

}