package vars

def call(Map config=[:]) {
    withCredentials([string(credentialsId: 'jfrog-user-token', variable: 'API_TOKEN')]) {
        // Bind config values as env vars and use a single-quoted sh script: the shell
        // resolves them from env rather than the script text being re-parsed, so values
        // containing shell metacharacters (e.g. `; rm -rf /`) can't inject commands.
        withEnv([
            "ARTIFACTORY_URL=${config.ARTIFACTORY_URL}",
            "REPO_NAME=${config.REPO_NAME}",
            "ARTIFACT_PATH=${config.ARTIFACT_PATH}",
            "FILE_PATH=${config.FILE_PATH}"
        ]) {
            sh '''
                curl -H "Authorization: Bearer $API_TOKEN" -T "$FILE_PATH" "$ARTIFACTORY_URL/artifactory/$REPO_NAME/$ARTIFACT_PATH"
            '''
        }
    }

}