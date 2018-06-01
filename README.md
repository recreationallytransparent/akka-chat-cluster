An experiment for integrating websockets, akka-streams, akka actors, and akka clustering with remoting

Decentralized, remote, fault tolerant, and simple chat app

to run:

```sbt run```

in project root. This will start the backend service

```
cd frontend
npm start
```

starts a web server on port 3000

You'll have to go into the js code, find where the backend server address is hardcoded, and change that value to the
terminal's output value. I'll change this eventually but right now c'est la vie