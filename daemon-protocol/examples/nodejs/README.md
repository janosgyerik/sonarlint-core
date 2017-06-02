Node.js example
===============

## Setup

### Get the daemon

Either get the latest zip from: https://sonarsource.bintray.com/Distribution/sonarlint-daemon/

Or build the `daemon` project with:

    mvn clean package -DskipTests -DskipDistWindows

### Run the daemon

Simply unzip and run the script in `bin/sonarlint-daemon`.

It listens on port 8050 by default.

### Install dependencies

Run:

    npm install

## Demo: analyze local file

Run:

    node demo-file.js

This should print something like:

    Found issue: { severity: 'BLOCKER',
      start_line: 3,
      start_line_offset: 0,
      end_line: 3,
      end_line_offset: 3,
      message: 'Add the "let", "const" or "var" keyword to this declaration of "arr" to make it explicit.',
      rule_key: 'javascript:S2703',
      rule_name: 'Variables should be declared explicitly',
      file_path: '/Users/janos/dev/git/github/sonarlint-core/daemon-protocol/examples/nodejs/bad.js',
      user_object: '' }
    status
    end of data

## Demo: analyze content

Run:

    node demo-content.sh
