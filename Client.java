import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public class Client {
    private int idxAB;
    private String tagAB;
    private SecretKey keyAB;
    private BulletinBoard board;

    public Client(BulletinBoard board, int initialIndex, String initialTag, SecretKey initialKey) {
        this.board = board;
        this.idxAB = initialIndex;
        this.tagAB = initialTag;
        this.keyAB = initialKey;
    }

    public void send(String message) throws Exception {
        System.out.println("Versturen bericht: " + message);
        System.out.println("Huidige index: " + idxAB + ", Tag: " + tagAB);

        int nextIndex = new Random().nextInt(board.size());
        String nextTag = UUID.randomUUID().toString();
        System.out.println("Nieuwe index: " + nextIndex + ", Nieuwe tag: " + nextTag);

        String payload = message + "||" + nextIndex + "||" + nextTag;
        byte[] encryptedMessage = encryptMessage(payload, keyAB);

        board.add(idxAB, encryptedMessage, tagAB);

        idxAB = nextIndex;
        tagAB = nextTag;
        keyAB = deriveKey(keyAB.getEncoded());
    }

    public String receive() throws Exception {
        System.out.println("Ophalen bericht bij index: " + idxAB + ", Tag: " + tagAB);
        byte[] encryptedMessage = board.get(idxAB, tagAB);
        if (encryptedMessage == null) {
            System.out.println("Geen bericht gevonden.");
            return null;
        }

        String decryptedMessage = decryptMessage(encryptedMessage, keyAB);
        System.out.println("Gedecrypteerd bericht: " + decryptedMessage);
        String[] parts = decryptedMessage.split("\\|\\|");
        idxAB = Integer.parseInt(parts[1]);
        tagAB = parts[2];
        keyAB = deriveKey(keyAB.getEncoded());

        return parts[0];
    }


    private byte[] encryptMessage(String message, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(message.getBytes());
    }

    private String decryptMessage(byte[] encryptedMessage, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(encryptedMessage));
    }

    private SecretKey deriveKey(byte[] previousKey) throws Exception {
        byte[] newKeyBytes = Arrays.copyOf(previousKey, 16); // Neem 16 bytes voor AES
        return new SecretKeySpec(newKeyBytes, "AES");
    }
}
