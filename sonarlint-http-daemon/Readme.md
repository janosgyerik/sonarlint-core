Steps to get it running:
* git clone git@github.com:bartfastiel/sonarlint-core.git
* cd sonarlint-core
* git checkout ugly-poc-sonarlint-online-httpservlet
* mvn clean install
* cd sonarlint-http-deamon
* mvn jetty:run
* open localhost:8080 in your browser
* or curl -XPOST localhost:8080/analyze -d 'yourcode'

