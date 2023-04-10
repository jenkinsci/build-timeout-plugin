#!/usr/bin/env groovy

/* For https://ci.jenkins.io/ */
/* The `buildPlugin` step has been provided by: https://github.com/jenkins-infra/pipeline-library */
buildPlugin(useContainerAgent: true, configurations: [
                [platform: 'linux', jdk: '11'],
                [platform: 'windows', jdk: '11'],
            ])
