def call(Map config=[:]) {
    withCredentials([string(credentialsId: 'jfrog-user-token', variable: 'API_TOKEN')]) {
        // Bind config values as env vars and use a single-quoted sh script: the shell
        // resolves them from env rather than the script text being re-parsed, so values
        // containing shell metacharacters (e.g. `; rm -rf /`) can't inject commands.
        withEnv([
            "ARTIFACTORY_URL=${config.ARTIFACTORY_URL}",
            "REPO_NAME=${config.REPO_NAME}",
            "ARTIFACT_PATH=${config.ARTIFACT_PATH}"
        ]) {
            // --fail: an HTTP error must not leave an error page behind as the "artifact".
            // The file is checked to be non-empty before it is archived.
            sh '''
                OUT="$(basename "$ARTIFACT_PATH")"
                curl --fail --silent --show-error --retry 3 --retry-connrefused \\
                     -H "Authorization: Bearer $API_TOKEN" -o "$OUT" \\
                     "$ARTIFACTORY_URL/artifactory/$REPO_NAME/$ARTIFACT_PATH"
                test -s "$OUT"
            '''
        }
        archiveArtifacts artifacts: '*.*', followSymlinks: false
    }

}
