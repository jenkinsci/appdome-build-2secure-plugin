/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/

pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                script {
                    // Set environment variables for the buildPlugin step
                    withEnv(['MY_VARIABLE=my_value', 'ANOTHER_VARIABLE=another_value']) {
                        // Call the buildPlugin step
                        buildPlugin(
                            useContainerAgent: true,
                            configurations: [
                                [platform: 'linux', jdk: 17], // use 'docker' if you have containerized tests
                                // [platform: 'windows', jdk: 11],
                            ]
                        )
                    }
                }
            }
        }

        // Add more stages as needed
    }
}
