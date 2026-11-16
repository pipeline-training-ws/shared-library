//NOT USED
def call(String ghtoken,String repo){
    branches=sh(script: """
            curl -s -L \
             -H "Accept: application/vnd.github+json" \
             -H "Authorization: Bearer ${ghtoken}" \
             -H "X-GitHub-Api-Version: 2022-11-28" \
             $repo  |jq -r '.[] | .name' | tr '\\n' ', ' | sed 's/,\$//'"""
            ,returnStdout: true)
    echo "BRANCHES: ${branches}"
    env.GIT_REPO_BRANCHES="${branches}"
    return branches
}