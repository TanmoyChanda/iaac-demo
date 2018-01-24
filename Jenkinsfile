pipeline {

  agent none
  stages {
    stage('Checkout Code'){
      steps {
        node('master') {
          deleteDir()
          checkout scm
          stash 'code'
        }
      }
    }
	stage('Create EC2 Instance') {
      steps {
        parallel(
          'Create EC2 Instance':{
            node('master') {
              deleteDir()
	      unstash 'code'
	      sh '${WORKSPACE}/wrapper.sh'
	      sh 'sleep 60'
		
            }
          }
        )
      }
    }
    stage('Build Application') {
      steps {
        parallel(
          'Build Application':{
            node('master') {
	       withMaven(maven: 'mvn3.2.5') {
		     deleteDir()
	     	     unstash 'code'
              	     sh 'mvn clean install'
		     archiveArtifacts 'target/*.war'
		    // stash name:'war_file', includes: '*.war'
		}
		
            }
          }
        )
      }
    }
    stage('Create & Deploy containerized App image on EC2') {
      steps {
        parallel(
          'create & deploy':{
            node('master') {
              deleteDir()
	      unstash 'code'
              sh 'echo "HELLO"'
	      sh '${WORKSPACE}/docker_create_deploy.sh'
            }
          }
        )
      }
    }

	stage('Testing') {
      steps {
        parallel(
          'Testing':{
            node('master') {
              deleteDir()
	      build 'integration_testing'
              sh 'echo `cat /var/tmp/publicip`:9080/Presentation-0.0.1-SNAPSHOT/'
            }
          }
        )
      }
    }

  }

   }
