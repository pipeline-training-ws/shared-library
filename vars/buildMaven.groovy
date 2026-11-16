def call (Map config) {
    // Test reports are published in finally so they exist even when a Maven step fails.
    // NOTE: pipeline-maven's withMaven may publish JUnit results itself depending on plugin
    // version/config; check for duplicate reports before keeping the explicit junit step.
    try {
        //https://plugins.jenkins.io/pipeline-maven/
        withMaven(
                //Use `$WORKSPACE/.repository` for local repository folder to avoid shared repositories
                //consider to use CloudBees Workspace caching https://docs.cloudbees.com/docs/cloudbees-ci/latest/pipelines/cloudbees-cache-step
                // mavenLocalRepo: '/tmp/.m2',
                // Maven settings.xml file defined with the Jenkins Config File Provider Plugin
                // We recommend to define Maven settings.xml globally at the folder level using
                // navigating to the folder configuration in the section "Pipeline Maven Configuration / Override global Maven configuration"
                // or globally to the entire master navigating to  "Manage Jenkins / Global Tools Configuration"
                //mavenSettingsConfig: 'global-maven-settings'
        ) {
            // plain for loop: closures that call steps (e.g. inside .collect) are a CPS risk
            // NOTE: steps come from the consumer's ci-config.yaml and run as shell commands by design;
            // restrict them (e.g. allow-list of Maven goals) if consumers are not trusted.
            for (String mavenStep : config.ci.maven.steps) {
                echo "maven step: ${mavenStep}"
                sh mavenStep
            }
            //https://docs.cloudbees.com/docs/cloudbees-ci/latest/pipelines/cloudbees-cache-step
            //writeCache name: 'maven-repo', includes: '.m2/repository/**', excludes: '**SNAPSHOT**'
        }
    } finally {
        junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'
    }
    archiveArtifacts '**/target/*.war'
}
