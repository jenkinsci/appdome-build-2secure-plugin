pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }
    agent {
        label 'linux'
    }
    stages {
        stage('build') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    sh '''
                        mvn -B -Djenkins.test.timeout=600 clean verify package
                    '''
                }
            }
        }
    }
}
