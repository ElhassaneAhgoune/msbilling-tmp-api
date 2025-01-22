pipeline {
    agent any
    environment {
        BRANCH_NAME = 'master'  // This automatically sets the branch name
    }
    stages {
        stage('Build') {
            steps {
                script {
                    echo "Building branch: ${BRANCH_NAME}"
                    sh 'mvn clean install -DskipTests=true'  // Or your build command here
                }
            }
        }
        stage('Deploy') {
            steps {
                script {
                    echo "Deploying branch: ${BRANCH_NAME}"
                    // Add your deployment steps here
                }
            }
        }
    }
}
