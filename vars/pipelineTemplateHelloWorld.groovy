def call(Map config) {
    pipeline {
        agent none
        options {
            // we dont need builddiscared here if glbal builddiscared is configured
            buildDiscarder(logRotator(numToKeepStr: '5'))
            //https://www.jenkins.io/blog/2018/02/22/cheetah/}
            //https://www.jenkins.io/doc/book/pipeline/scaling-pipeline/
            durabilityHint('PERFORMANCE_OPTIMIZED')
            timeout(time: 1, unit: 'HOURS')
        }
        stages {
            stage('CI') {
                agent {
                    kubernetes {
                        //use the yaml file ref from ci-user-config, rendered with the configured images
                        yaml libraryResource("podtemplates/agent.yaml")
                        //TODO: RENAME DEFAULT CONTAONER
                        defaultContainer "maven"
                    }
                }
                stages {
                    stage("Hello World") {
                        steps {
                            sh "echo Hello ${config.hello}"
                        }
                    }
                    stage("Hi") {
                        when {
                            branch 'main'
                        }
                        steps {
                            sh "echo Hi ${config.firstName}  ${config.lastName}"
                        }
                    }
                }
            }            
        }
    }
}