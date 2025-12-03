pipeline {
    agent any
    
    environment {
        PROJECT_NAME = 'probability-project'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code...'
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                echo 'Building project...'
                // sh 'mvn clean install'
                // sh 'gradle build'
            }
        }
        
        stage('Test') {
            steps {
                echo 'Running tests...'
                // sh 'mvn test'
                // sh 'gradle test'
            }
        }
        
        stage('Code Quality') {
            steps {
                echo 'Analyzing code quality...'
                // sh 'mvn sonar:sonar'
            }
        }
        
        stage('Package') {
            steps {
                echo 'Packaging application...'
                // sh 'mvn package'
            }
        }
        
        stage('Deploy') {
            steps {
                echo 'Deploying application...'
                // Agrega aquí tus comandos de deploy
            }
        }
    }
    
    post {
        success {
            echo 'Pipeline ejecutado exitosamente!'
        }
        failure {
            echo 'Pipeline falló. Revisa los logs.'
        }
        always {
            cleanWs()
        }
    }
}