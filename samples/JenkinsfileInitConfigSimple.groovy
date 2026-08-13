library identifier: 'ci-shared-library@main', retriever: modernSCM(
        [$class: 'GitSCMSource',
         remote: 'https://github.com/cb-ci-templates/ci-shared-library.git'])


// Building the data object
def configYaml = """---
app : 'Hello World'
k8_agent_yaml : 'podTemplate-maven.yaml'
branchPropertiesFile: 'ci-config.yaml'
"""

Map configMap = readYaml text: "${configYaml}"
println configMap
pipelineInitConfigSimple (configMap)
