node {
  stage('SCM') {
    checkout scm
  }

  stage('SonarQube Analysis') {
    def jdk = tool 'JDK 21'
    def mvn = tool 'Default Maven'

    withEnv(["JAVA_HOME=${jdk}", "PATH+JAVA=${jdk}/bin"]) {
      withSonarQubeEnv('SonarQube') {
        dir('SpringAiDemo') {
          sh "${mvn}/bin/mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=SpringAiChatBox"
        }
      }
    }
  }
}