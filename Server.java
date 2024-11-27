import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class Server {
    public static void main(String[] args) {
        try {
            // Start the RMI registry programmatically on port 1099
            LocateRegistry.createRegistry(1099);

            // Create the BulletinBoard instance
            BulletinBoardInterface board = new BulletinBoard(100);
            Naming.rebind("rmi://localhost/BulletinBoard", board);

            // Optional: Host a Key Exchange Service for secure initializations
            KeyExchangeServer keyExchangeServer = new KeyExchangeServer();
            Naming.rebind("rmi://localhost/KeyExchangeService", keyExchangeServer);

            System.out.println("Bulletin Board RMI Server and Key Exchange Service are running...");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
