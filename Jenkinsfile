node {
    stage('sbt build') {
        sbt 'set test in assembly := {}' clean assembly
    }

    stage('unittests') {
        sbt test
    }
}