Steps to get it running:
* git clone git@github.com:bartfastiel/sonarlint-core.git
* cd sonarlint-core
* git checkout ugly-poc-sonarlint-online-httpservlet
* mvn clean install
* cd sonarlint-http-daemon
* cd plugins/
* wget https://sonarsource.bintray.com/Distribution/sonar-javascript-plugin/sonar-javascript-plugin-3.0.0.4962.jar
* cd ..
* add this to your maven settings.xml:
```xml
<pluginGroups>
  <pluginGroup>org.mortbay.jetty</pluginGroup>
</pluginGroups>
```
* mvn jetty:run
* ignore all those command line warnings (wait for `[INFO] Started Jetty Server`)
* open localhost:8080 in your browser
* or curl -XPOST localhost:8080/analyze -d 'yourcode'

