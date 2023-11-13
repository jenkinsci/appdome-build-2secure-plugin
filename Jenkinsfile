/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/

pipeline {
    agent any

    stages {
        stage('Build') {
            options {
                timeout(time: 15, unit: 'MINUTES') // Set timeout to 15 minutes
            }
            steps {
                script {
                    echo "Starting build!"

                    // Call the buildPlugin step
                    buildPlugin(
                        useContainerAgent: true,
                        configurations: [
                            [platform: 'linux', jdk: 17], // use 'docker' if you have containerized tests
                            // [platform: 'windows', jdk: 11],
                        ]
                    )
                    echo "Build completed successfully!"
                }
            }
        }

        // Add more stages as needed
    }
}
