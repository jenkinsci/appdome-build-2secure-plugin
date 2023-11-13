/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/
    environment {
        // Set environment variables
        MY_VARIABLE = 'my_value'
        ANOTHER_VARIABLE = 'another_value'
    }

buildPlugin(
  useContainerAgent: true,
  configurations: [
    [platform: 'linux', jdk: 17], // use 'docker' if you have containerized tests
//     [platform: 'windows', jdk: 11],
])
