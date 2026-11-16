//will set an environmental variable to short commit, must of git repo in workspace
def call(size = 7) {
    int n = (size ?: 7) as Integer
    if (n < 4 || n > 40) {
        error "gitShortCommit: size must be between 4 and 40"
    }
    env.SHORT_COMMIT = sh(returnStdout: true, script: "git rev-parse HEAD | cut -c1-${n} | tr -d '\\n'")
    env.COMMIT_AUTHOR = sh(returnStdout: true, script: 'git --no-pager show -s --format=%an "$SHORT_COMMIT" | tr -d "\\n"')
    echo "SHORT_COMMIT: ${env.SHORT_COMMIT}"
    echo "COMMIT_AUTHOR: ${env.COMMIT_AUTHOR}"
    //sh "env |sort"
}
