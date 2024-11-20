import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class Main {
    public static void main(String[] args) throws Exception {
        // Maak het bulletin board
        BulletinBoard board = new BulletinBoard(100);

        // Genereer een initiële sleutel
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey initialKey = keyGen.generateKey();

        // Maak twee clients
        Client alice = new Client(board, 0, "initialTagAlice", initialKey);
        Client bob = new Client(board, 0, "initialTagBob", initialKey);

        // Alice verstuurt een bericht
        alice.send("Hallo Bob!");

        // Bob ontvangt het bericht
        String receivedMessage = bob.receive();
        System.out.println("Bob ontving: " + receivedMessage);
    }
}
