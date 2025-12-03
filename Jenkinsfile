pipeline {
    agent any

    environment {
        PROJECT_NAME = 'probability-project'
        STACK_NAME = 'probabilidad-stack'
        BACKEND_IMAGE = 'quarkus-probabilidad'
        FRONTEND_IMAGE = 'frontend-probabilidad'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code...'
                checkout scm
            }
        }

        stage('Load Environment Variables') {
            steps {
                script {
                    echo 'Loading environment variables from Jenkins secret file...'
                    withCredentials([file(credentialsId: 'stack-probability-env-file', variable: 'ENV_FILE')]) {
                        sh 'cp $ENV_FILE infra/.env'
                        echo 'Environment variables loaded successfully'
                    }
                }
            }
        }

        stage('Load Keycloak Configuration') {
            steps {
                script {
                    echo 'Loading Keycloak realm configuration from Jenkins secret file...'
                    withCredentials([file(credentialsId: 'keycloak-realm-file', variable: 'REALM_FILE')]) {
                        sh '''
                            mkdir -p infra/keycloak/realms
                            cp $REALM_FILE infra/keycloak/realms/Probabilidad-realm.json
                            echo "Keycloak realm configuration loaded"
                        '''
                    }
                }
            }
        }

        stage('Build Backend Image') {
            steps {
                script {
                    echo "Building backend image: ${BACKEND_IMAGE}:latest"
                    dir('backend') {
                        sh """
                            docker build \
                                -t ${BACKEND_IMAGE}:latest \
                                -f Dockerfile \
                                .
                        """
                    }
                    echo "Backend image built successfully"
                }
            }
        }

        stage('Build Frontend Image') {
            steps {
                script {
                    echo "Building frontend image: ${FRONTEND_IMAGE}:latest"
                    withCredentials([file(credentialsId: 'stack-probability-env-file', variable: 'ENV_FILE')]) {
                        dir('frontend') {
                            sh """#!/bin/bash
                                set -a
                                source \$ENV_FILE
                                set +a
                                docker build \
                                    --build-arg VITE_KEYCLOAK_URL=\${VITE_KEYCLOAK_URL:-https://keycloak.glud.org} \
                                    --build-arg VITE_KEYCLOAK_REALM=\${VITE_KEYCLOAK_REALM:-Probabilidad} \
                                    --build-arg VITE_KEYCLOAK_CLIENT_ID=\${VITE_KEYCLOAK_CLIENT_ID:-frontend-spa} \
                                    --build-arg VITE_API_BASE=\${VITE_API_BASE:-https://probabilidadapi.glud.org/api} \
                                    -t ${FRONTEND_IMAGE}:latest \
                                    -f Dockerfile \
                                    .
                            """
                        }
                    }
                    echo "Frontend image built successfully"
                }
            }
        }

        stage('Validate Docker Stack Configuration') {
            steps {
                script {
                    echo 'Validating docker-stack.yml configuration...'

                    withCredentials([file(credentialsId: 'stack-probability-env-file', variable: 'ENV_FILE')]) {
                        dir('infra') {
                            sh """#!/bin/bash
                                # Cargar variables de entorno
                                set -a
                                source \$ENV_FILE
                                set +a

                                # Validar sintaxis del docker-stack.yml
                                docker-compose -f docker-stack.yml config > /dev/null
                                echo "Docker stack configuration is valid"

                                # Verificar que las imágenes existen localmente
                                echo "Checking if required images exist..."
                                docker images | grep ${BACKEND_IMAGE} || echo "⚠ Warning: Backend image not found"
                                docker images | grep ${FRONTEND_IMAGE} || echo "⚠ Warning: Frontend image not found"
                            """
                        }
                    }
                    echo 'Validation completed successfully'
                }
            }
        }

        stage('Deploy to Swarm') {
            steps {
                script {
                    echo "Deploying stack: ${STACK_NAME}"

                    withCredentials([file(credentialsId: 'stack-probability-env-file', variable: 'ENV_FILE')]) {
                        dir('infra') {
                            sh """#!/bin/bash
                                # Cargar variables de entorno para el deploy
                                set -a
                                source \$ENV_FILE
                                set +a

                                if ! docker info | grep -q "Swarm: active"; then
                                    echo "Warning: Docker Swarm is not active. Initializing..."
                                    docker swarm init || echo "Already in swarm mode"
                                fi

                                # Deploy del stack
                                docker stack deploy \
                                    --compose-file docker-stack.yml \
                                    --prune \
                                    ${STACK_NAME}

                                echo "Stack deployed successfully"

                                # Esperar un momento para que los servicios se inicialicen
                                sleep 20

                                # Mostrar estado de los servicios
                                echo "Service status:"
                                docker stack services ${STACK_NAME}
                            """
                        }
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    echo 'Performing health checks...'

                    sh """
                        echo "Waiting for services to be ready..."
                        sleep 10

                        # Verificar estado de los servicios
                        docker stack ps ${STACK_NAME} --filter "desired-state=running"

                        # Verificar que todos los servicios estén corriendo
                        EXPECTED_SERVICES=4
                        RUNNING_SERVICES=\$(docker stack services ${STACK_NAME} --format "{{.Replicas}}" | grep -c "1/1" || true)

                        echo "Expected services: \$EXPECTED_SERVICES"
                        echo "Running services: \$RUNNING_SERVICES"

                        if [ "\$RUNNING_SERVICES" -lt "\$EXPECTED_SERVICES" ]; then
                            echo "Warning: Not all services are running yet"
                            docker stack services ${STACK_NAME}
                        else
                            echo "All services are running"
                        fi
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline ejecutado exitosamente'
            echo "Stack '${STACK_NAME}' deployed with images:"
            echo "  - ${BACKEND_IMAGE}:latest"
            echo "  - ${FRONTEND_IMAGE}:latest"
        }
        failure {
            echo 'Pipeline falló. Revisa los logs.'
            script {
                sh """
                    echo "Service logs:"
                    docker stack services ${STACK_NAME} || true
                    docker stack ps ${STACK_NAME} --no-trunc || true
                """
            }
        }
        always {
            sh '''
                rm -f infra/.env || true
                rm -f infra/keycloak/realms/Probabilidad-realm.json || true
            '''
            sh 'docker image prune -f || true'
        }
    }
}
