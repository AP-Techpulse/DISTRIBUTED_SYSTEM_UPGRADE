/**
 * Packet represents a unit of data travelling between network routers.
 * Mirrors the Packet.java class described in the course document.
 */
public class Packet {
    private final String id;
    private final String source;
    private final String destination;

    public Packet(String id, String source, String destination) {
        this.id          = id;
        this.source      = source;
        this.destination = destination;
    }

    public String getId()          { return id; }
    public String getSource()      { return source; }
    public String getDestination() { return destination; }

    @Override
    public String toString() {
        return "[" + id + ": " + source + " → " + destination + "]";
    }
}
