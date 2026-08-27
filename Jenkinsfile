pipeline {
    agent any
    tools {
        jdk "JDK21"
        maven "Maven3"
    }
    stages {  
        stage('Checkout') { 
            steps { 
                checkout scm 
            } 
        }
        stage('Build') {
            steps {
                sh 'mvn -B clean package'
            }
        }
        stage('Build Image') {
            steps { sh 'docker build -t mission-day:${BUILD_NUMBER} .' }
        }
        stage('Smoke Test') {
            steps { sh 'docker run --rm mission-day:${BUILD_NUMBER}' }
        }
        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

    }
}