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
                      // Execute 'ls' command and print the result
                        def output = sh(script: 'ls', returnStdout: true).trim()
                        echo "Directory Listing: \n${output}"
                        // Load the custom Groovy script
                        def customBuildPlugin = load 'src/test/java/io/jenkins/plugins/appdome/build/to/secure/BuildPlugin.groovy'
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
