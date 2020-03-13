pipeline {
    environment {
        QUARKUS_VERSION = '1.3.0.Final'
    }
    options {
        ansiColor("xterm")
    }
    agent { label 'ocp_executor' }
    stages {
        stage('Login') {
            steps {
                sh "oc login ${OCP_URL}  --username=${OCP_USERNAME} --password=${OCP_PASSWORD}"
            }
        }
        stage('Run tests') {
            steps {
                sh "./mvnw clean verify -Dversion.quarkus=${QUARKUS_VERSION} -Dts.use-ephemeral-namespaces"
            }
        }
    }
    post {
        always {
            junit '**/target/*-reports/*.xml'
        }
    }
}
