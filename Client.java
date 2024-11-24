import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.rmi.Naming;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public class Client {
    private int idxAB;
    private String tagAB;
    private SecretKey keyAB;
    private BulletinBoardInterface board;

    public Client(String serverAddress, int initialIndex, String initialTag, SecretKey initialKey) throws Exception {
        this.board = (BulletinBoardInterface) Naming.lookup("rmi://" + serverAddress + "/BulletinBoard");
        this.idxAB = initialIndex;
        this.tagAB = initialTag;
        this.keyAB = initialKey;
    }

    public void send(String message) throws Exception {
        int nextIndex = new Random().nextInt(board.size());
        String nextTag = UUID.randomUUID().toString();
        String payload = message + "||" + nextIndex + "||" + nextTag;
        byte[] encryptedMessage = encryptMessage(payload, keyAB);

        board.add(idxAB, encryptedMessage, tagAB);

        idxAB = nextIndex;
        tagAB = nextTag;
        keyAB = deriveKey(keyAB.getEncoded());
    }

    public String receive() throws Exception {
        byte[] encryptedMessage = board.get(idxAB, tagAB);
        if (encryptedMessage == null) {
            return null;
        }

        String decryptedMessage = decryptMessage(encryptedMessage, keyAB);
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

    private SecretKey deriveKey(byte[] previousKey) {
        byte[] newKeyBytes = Arrays.copyOf(previousKey, 16);
        return new SecretKeySpec(newKeyBytes, "AES");
    }
}
