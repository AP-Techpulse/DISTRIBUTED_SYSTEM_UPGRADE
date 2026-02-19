# Q1 — Multi-Client RMI (4 Clients)

## Compile
```
javac -encoding UTF-8 ServerInterface.java RMIServer.java RMIClient.java
```

## Run
Open **5 terminals** (or 5 CMD/PowerShell windows):

**Terminal 1 — Server (run FIRST)**
```
java RMIServer
```

**Terminals 2–5 — Four Clients**
```
java RMIClient Alice
java RMIClient Bob
java RMIClient Carol
java RMIClient Dave
```

## What you'll see
- Server window shows a live log of every request, including which client called which method
- Each client gets a differently coloured window
- All 4 clients share the same resource list (click "📁 View Resources")
- Any client can add a resource and others will see it immediately
- Click "🌟 Personality Test" to fill in a form — the server generates a personality summary
