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
                timeout(time: 10, unit: 'MINUTES') {
                    sh '''
                        mvn -B -ntp clean verify package
                    '''
                }
            }
        }
    }
}
