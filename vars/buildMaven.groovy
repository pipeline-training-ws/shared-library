def call (Map config) {
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
            //sh config.ci.maven.steps[1]
            script {
                config.ci.maven.steps.collect { step ->
                    println step
                    sh step
                }
            }
            //https://docs.cloudbees.com/docs/cloudbees-ci/latest/pipelines/cloudbees-cache-step
            //writeCache name: 'maven-repo', includes: '.m2/repository/**', excludes: '**SNAPSHOT**'
        }
        junit '**/target/surefire-reports/TEST-*.xml'
        archiveArtifacts '**/target/*.war'
}