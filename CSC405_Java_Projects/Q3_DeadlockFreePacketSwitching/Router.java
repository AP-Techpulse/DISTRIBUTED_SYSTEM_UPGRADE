import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.function.Consumer;

/**
 * Router models a network node with a finite buffer.
 *
 * Deadlock Prevention Strategy — Lock Ordering (Resource Hierarchy):
 *   When two routers need to exchange a packet, they ALWAYS acquire their
 *   internal locks in alphabetical order of their IDs.
 *   This breaks the "circular wait" condition required for a deadlock.
 *
 *   Example without ordering: A locks itself, B locks itself → deadlock.
 *   With ordering: both threads try to lock A first → one waits, no deadlock.
 */
public class Router {

    private final String routerId;
    private final ReentrantLock lock = new ReentrantLock();

    // Finite-size buffer — simulates the document's "buffer size 4" example
    private final int bufferCapacity;
    private final Map<String, Packet> buffer = new LinkedHashMap<>();

    // Neighbour routers this router can forward to
    private final Map<String, Router> neighbors = new HashMap<>();

    // Callback so the GUI can receive log messages
    private Consumer<String> logCallback = System.out::println;

    public Router(String routerId, int bufferCapacity) {
        this.routerId       = routerId;
        this.bufferCapacity = bufferCapacity;
    }

    public void setLogCallback(Consumer<String> cb) { this.logCallback = cb; }
    public void addNeighbor(String id, Router r)    { neighbors.put(id, r); }
    public String getRouterId()                      { return routerId; }
    public int    getBufferSize()                    { return buffer.size(); }
    public int    getBufferCapacity()                { return bufferCapacity; }
    public List<Packet> getBufferContents() {
        lock.lock();
        try { return new ArrayList<>(buffer.values()); }
        finally { lock.unlock(); }
    }

    // ───────────────────────  Receive  ───────────────────────────────────────

    /**
     * Accepts a packet into the local buffer.
     * Returns false if the buffer is full (drop packet → prevents deadlock by
     * ensuring buffers never block indefinitely waiting for space).
     */
    public boolean receivePacket(Packet packet) {
        lock.lock();
        try {
            if (buffer.size() >= bufferCapacity) {
                log("⚠️  BUFFER FULL — dropped packet " + packet.getId());
                return false;
            }
            buffer.put(packet.getId(), packet);
            log("📥 Received " + packet + " (buffer: " + buffer.size() + "/" + bufferCapacity + ")");
            return true;
        } finally {
            lock.unlock();
        }
    }

    // ───────────────────────  Forward  ───────────────────────────────────────

    /**
     * Forwards a packet to the given neighbour router.
     *
     * DEADLOCK PREVENTION: locks are always acquired in alphabetical ID order.
     * So if Router A forwards to B and B forwards to A simultaneously,
     * both threads first try to acquire A's lock → one of them waits →
     * no circular wait → no deadlock.
     */
    public boolean forwardPacket(Packet packet, String nextRouterId) {
        Router next = neighbors.get(nextRouterId);
        if (next == null) {
            log("❌ Error: Neighbour " + nextRouterId + " not found");
            return false;
        }

        // --- Lock Ordering (alphabetical by router ID) ---
        Router first  = routerId.compareTo(next.routerId) < 0 ? this : next;
        Router second = (first == this) ? next : this;

        first.lock.lock();
        try {
            second.lock.lock();
            try {
                if (!buffer.containsKey(packet.getId())) {
                    log("⚠️  Packet " + packet.getId() + " not in buffer (already forwarded?)");
                    return false;
                }
                buffer.remove(packet.getId());
                boolean accepted = next.receivePacket(packet);
                if (accepted) {
                    log("📤 Forwarded " + packet + " → Router " + next.routerId);
                } else {
                    // Put it back if next router's buffer was full
                    buffer.put(packet.getId(), packet);
                    log("↩️  Forwarding failed (next buffer full) — packet returned");
                }
                return accepted;
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }

    private void log(String msg) {
        logCallback.accept("[Router " + routerId + "] " + msg);
    }
}
