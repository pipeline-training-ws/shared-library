package vars

def call(Map config=[:]) {
    withCredentials([string(credentialsId: 'jfrog-user-token', variable: 'API_TOKEN')]) {

        sh """
            curl -H "Authorization: Bearer ${API_TOKEN}" -O "${config.ARTIFACTORY_URL}/artifactory/${config.REPO_NAME}/${config.ARTIFACT_PATH}"
        """
        archiveArtifacts artifacts: '*.*', followSymlinks: false
    }

}