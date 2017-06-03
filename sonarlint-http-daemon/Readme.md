Steps to get it running:
* git clone git@github.com:bartfastiel/sonarlint-core.git
* cd sonarlint-core
* git checkout ugly-poc-sonarlint-online-httpservlet
* mvn clean install
* cd sonarlint-http-deamon
* put analyzer jars into plugins/ directory
* mvn jetty:run
* ignore all those command line warnings (wait for `[INFO] Started Jetty Server`)
* open localhost:8080 in your browser
* or curl -XPOST localhost:8080/analyze -d 'yourcode'

