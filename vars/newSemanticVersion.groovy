def call(Map params){
    writeFile encoding: 'utf-8', file: "${WORKSPACE}/nsv.sh", text: libraryResource ('newSemanticVersion/newSemanticVersion.sh')
    def newSemanticVersion=sh(script: "chmod a+x ${WORKSPACE}/nsv.sh && ${WORKSPACE}/nsv.sh ${params.arg} ${params.version}",returnStdout: true)
    echo "New Version: ${newSemanticVersion}"
    env.NEW_SEMANTIC_VERSION="${newSemanticVersion}"
    return newSemanticVersion
}