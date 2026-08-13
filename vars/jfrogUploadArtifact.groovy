package vars

def call(Map config=[:]) {
    withCredentials([string(credentialsId: 'jfrog-user-token', variable: 'API_TOKEN')]) {

        sh """
            curl -H "Authorization: Bearer ${API_TOKEN}" -T "${config.FILE_PATH}" "${config.ARTIFACTORY_URL}/artifactory/${config.REPO_NAME}/${config.ARTIFACT_PATH}"
        """
    }

}