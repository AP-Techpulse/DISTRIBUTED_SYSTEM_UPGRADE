# Q2 — Content-Based Routing (Apache Camel Simulation)
## Compile & Run
```
cd Q2_ContentBasedRouting
javac -encoding UTF-8 ContentBasedRouter.java
java ContentBasedRouter
```
## What you'll see
- Left panel: submit a custom message (filename, type, content)
- Centre: live routing log showing each routing decision with reasoning
- Right panel: live bar chart of how many messages went to each queue
- "⚡ Run All Demo" button routes 8 preset messages with a 0.6s delay so you see each one appear

---

# Q3 — Deadlock-Free Packet Switching
## Compile & Run
```
cd Q3_DeadlockFreePacketSwitching
javac -encoding UTF-8 Packet.java Router.java PacketSwitchingGUI.java
java PacketSwitchingGUI
```
## What you'll see
- Four router boxes (A, B, C, D) connected in a ring
- "📨 Send Packet" injects a packet at the chosen router
- "⏩ Forward All" moves all packets one hop forward (around the ring)
- "🔥 Simulate Deadlock Attempt" — spawns two threads trying to swap packets between A and D simultaneously; lock ordering prevents the deadlock
- Buffer levels shown live inside each router card

---

# Q4 — Network Simulation
## Compile & Run
```
cd Q4_NetworkSimulation
javac -encoding UTF-8 NetworkSimulationGUI.java
java NetworkSimulationGUI
```
## What you'll see
- Animated network canvas with nodes and links
- Switch between Ring / Star / Mesh topologies
- Animated coloured dots travel along links when packets are forwarded
- "⚡ Auto-Simulate" sends 10 random packets automatically
- Stats counter (Sent / Delivered / Dropped) updates in real time
