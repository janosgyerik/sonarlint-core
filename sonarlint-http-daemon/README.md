### Setup

Add this to your `~/.m2/settings.xml` (inside `<settings/>`):

```xml
<pluginGroups>
  <pluginGroup>org.mortbay.jetty</pluginGroup>
</pluginGroups>
```

### Get, build and run

Clone and build sonarlint-core:

    git clone git@github.com:bartfastiel/sonarlint-core.git
    cd sonarlint-core
    git checkout ugly-poc-sonarlint-online-httpservlet
    mvn clean install

Download analyzers:

    cd sonarlint-http-daemon/plugins
    wget https://sonarsource.bintray.com/Distribution/sonar-javascript-plugin/sonar-javascript-plugin-3.0.0.4962.jar
    wget https://sonarsource.bintray.com/Distribution/sonar-python-plugin/sonar-python-plugin-1.8.0.1496.jar
    wget https://sonarsource.bintray.com/Distribution/sonar-php-plugin/sonar-php-plugin-2.10.0.2087.jar
    cd ..

Run:

    mvn jetty:run

Open http://localhost:8080 in your browser,
or curl -X POST localhost:8080/analyze -d language=javascript -d yourcode

