var grpc = require('grpc');
var protoPath = __dirname + '/../../src/main/proto/sonarlint-daemon.proto';
var proto = grpc.load(protoPath).sonarlint;

var client = new proto.StandaloneSonarLint('localhost:8050', grpc.credentials.createInsecure());

var content = `
arr = [1, 2, 3];
for (var x in arr) {
	console.log(x);
}
`;

var req = {
  content: content,
  language: 'JavaScript',
  charset: 'UTF-8'
};

var call = client.analyzeContent(req);

call.on('data', function(issue) {
  console.log('Found issue:', issue);
});
call.on('end', function() {
  console.log('end of data');
});
call.on('status', function(status) {
  console.log('status');
});
