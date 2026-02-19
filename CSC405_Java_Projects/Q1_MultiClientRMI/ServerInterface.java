import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * ServerInterface defines the remote methods clients can call over RMI.
 * Any method here can be invoked from a different JVM (client machine).
 */
public interface ServerInterface extends Remote {

    /** Returns the list of shared resources available to all clients */
    List<String> getSharedResources() throws RemoteException;

    /** Adds a resource to the shared list (visible to all clients) */
    void addSharedResource(String resource) throws RemoteException;

    /**
     * Generates a fun personality summary based on the client's info.
     * @param clientName - the name of the calling client
     * @param fullName   - person's full name
     * @param age        - person's age
     * @param hobbies    - comma-separated hobbies
     * @param favColor   - favourite colour
     */
    String generatePersonality(String clientName, String fullName,
                               int age, String hobbies,
                               String favColor) throws RemoteException;

    /** Returns a log of all requests received by the server */
    List<String> getServerLog() throws RemoteException;
}
