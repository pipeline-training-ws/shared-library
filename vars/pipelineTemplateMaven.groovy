def call(Map configDefaults) {
    Map config = [:]
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
            stage('Init') {
                agent {
                    kubernetes {
                        yaml libraryResource("podtemplates/podTemplate-init.yaml")
                    }
                }
                steps {
                    //TODO remove script
                    script {
                        config = init configDefaults
                    }
                }
            }
            stage('CI') {
                agent {
                    kubernetes {
                        //use the yaml file ref from ci-user-config, rendered with the configured images
                        yaml initPodTemplate(config)
                        //TODO: RENAME DEFAULT CONTAONER
                        defaultContainer "maven"
                    }
                }
                stages {
                    stage("build") {
                        steps {
                            routerBuild config
                        }
                    }
                    stage("Image") {
                        when {
                            branch 'main'
                        }
                        steps {
                            routerBuildImage config
                        }
                    }
                    stage("test") {
                        steps {
                            sh "echo image "
                        }
                    }
                    stage("qa scans") {
                        steps {
                            parallel(
                                    Sonar: {
                                container("maven") {
                                    echo "echo sonar scan"
                                    }
                            },
                                    RoxCtL: {
                                        container("maven") {
                                            echo "roxctl scan"
                                        }
                                    }
                            )
                        }
                    }
                }
            }
            stage('CD') {
                agent {
                    kubernetes {
                        yaml initPodTemplate(config)
                        defaultContainer 'maven'
                        showRawYaml true
                    }
                }
                stages {
                    stage("deploy") {
                        steps {
                            sh "echo deploy "
                        }
                    }
                    stage("test") {
                        steps {
                            sh "echo test "
                        }
                    }
                }
            }
        }
    }
}