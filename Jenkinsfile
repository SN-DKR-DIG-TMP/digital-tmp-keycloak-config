pipeline {
    agent any

    environment {
        NAME = 'digital-tmp-keycloak-config'
        VERSION = readMavenPom().getVersion()
        EMAIL = 'noreply@mycompay.com'
    }

    options {
      timeout(time: 120, unit: 'MINUTES')
    }

    stages {

        stage('Clean Install') {
            steps {
                sh  "mvn clean install"
                stash includes: '**/target/*', name: 'target'
            }

        }

        /*stage('SonarQube Scan') {
          steps{
            script{
              withSonarQubeEnv('SONAR_DEPLOY') {
                sh '/usr/local/maven/bin/mvn sonar:sonar -X'
              }
            }
          }
        }*/

       /* stage("SonarQube Quality Gate") {
              steps{
                sleep 120
                  script{
                    timeout(time: 15, unit: 'MINUTES') {
                       def qg = waitForQualityGate()
                       if (qg.status != 'OK') {
                          error "Pipeline aborted due to quality gate failure: ${qg.status}"
                       }
                    }
                  }
              }
        }

        stage('Snapshot On Nexus') {
            when { branch 'develop' }
            steps {
                sh  "mvn deploy"
            }
        } */



    }

    post {

       changed {
            emailext attachLog: true, body: '$DEFAULT_CONTENT', subject: '$DEFAULT_SUBJECT',  to: '${EMAIL}'
       }
        failure {
            emailext attachLog: true, body: '$DEFAULT_CONTENT', subject: '$DEFAULT_SUBJECT',  to: '${EMAIL}'
        }

    }
}
