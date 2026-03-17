pipeline {
  agent any

  parameters {
    string(name: 'ref', defaultValue: 'refs/heads/main', description: 'Git ref')
    string(name: 'sha', defaultValue: 'local-test-sha', description: 'Commit SHA')
    string(name: 'repository', defaultValue: 'local/FundingBoost', description: 'Repository name')
    string(name: 'actor', defaultValue: 'local', description: 'Trigger actor')
  }

  stages {
    stage('Print Event') {
      steps {
        sh '''
          set -eu
          echo "ref=${ref}"
          echo "sha=${sha}"
          echo "repository=${repository}"
          echo "actor=${actor}"
        '''
      }
    }

    stage('Deploy') {
      steps {
        sh '''
          set -eu
          bash /workspace/fundingboost/deploy/jenkins/scripts/local-deploy.sh \
            "${ref}" \
            "${sha}" \
            "${repository}" \
            "${actor}"
        '''
      }
    }
  }
}
