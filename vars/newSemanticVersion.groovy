// cfg.arg: one of -M (major), -m (minor), -p (patch); cfg.version: MAJOR.MINOR.PATCH
def call(Map cfg) {
    if (!(cfg.arg in ['-M', '-m', '-p'])) {
        error "newSemanticVersion: arg must be one of -M, -m, -p"
    }
    if (!(cfg.version ==~ /^\d+\.\d+\.\d+$/)) {
        error "newSemanticVersion: version must be MAJOR.MINOR.PATCH"
    }
    writeFile encoding: 'utf-8', file: 'nsv.sh', text: libraryResource('newSemanticVersion/newSemanticVersion.sh')
    def newSemanticVersion = ''
    withEnv(["NSV_ARG=${cfg.arg}", "NSV_VERSION=${cfg.version}"]) {
        // values are resolved by the shell from env, never interpolated into the script text
        newSemanticVersion = sh(script: 'bash ./nsv.sh "$NSV_ARG" "$NSV_VERSION"', returnStdout: true).trim()
    }
    echo "New Version: ${newSemanticVersion}"
    env.NEW_SEMANTIC_VERSION = newSemanticVersion
    return newSemanticVersion
}
