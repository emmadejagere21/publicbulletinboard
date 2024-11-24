import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class Server {
    public static void main(String[] args) {
        try {
            // Start the RMI registry programmatically on port 1099
            LocateRegistry.createRegistry(1099);

            // Create the BulletinBoard instance
            BulletinBoardInterface board = new BulletinBoard(100);

            // Bind the service to a name
            Naming.rebind("rmi://localhost/BulletinBoard", board);

            System.out.println("Bulletin Board RMI Server is running...");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
