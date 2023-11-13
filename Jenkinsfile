pipeline {
    options {
        timestamps()
        disableConcurrentBuilds(abortPrevious: true)
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }
    agent {
        label 'linux'
    }
    stages {
        stage('build') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    script {
                        // Load the custom Groovy script
                        def customBuildPlugin = load 'src/test/java/BuildPlugin.groovy'
                        // Define the parameters for the customBuildPlugin
                        Map params = [
                            repo: 'https://github.com/jenkinsci/appdome-build-2secure-plugin',
                            failFast: true,
                            timeout: 200, // set your custom timeout here
                            gitDefaultBranch: 'main',
                            useArtifactCachingProxy: true,
                            useContainerAgent: true,
                            platforms: ['linux'], // Ensure this is a list
                            jdkVersions: [17] // Ensure this is a list
                        ]
                        // Call the custom method with the parameters
                        customBuildPlugin(params)
                    }
                }
            }
        }
    }
}
