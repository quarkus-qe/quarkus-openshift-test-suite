pipeline {
    environment {
        QUARKUS_VERSION = '1.3.0.Alpha1'
    }
    options {
        ansiColor("xterm")
    }
    agent { label 'ocp_executor' }
    stages {
        stage('Run tests') {
            steps {
                sh "./mvnw clean verify -Dversion.quarkus=${QUARKUS_VERSION}"
            }
        }
    }
    post {
        always {
            junit '**/target/*-reports/*.xml'
        }
    }
}