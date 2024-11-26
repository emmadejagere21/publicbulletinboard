import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.rmi.Naming;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.security.SecureRandom;

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
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv); // IV is randomly generated
        GCMParameterSpec spec = new GCMParameterSpec(128, iv); //128 bit auth tag length
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] encryptedMessage = cipher.doFinal(message.getBytes()); // Encrypt the message
        byte[] result = new byte[iv.length + encryptedMessage.length]; // Combine IV and encrypted message
        System.arraycopy(iv, 0, result, 0, iv.length); // Copy IV to the beginning of the result
        System.arraycopy(encryptedMessage, 0, result, iv.length, encryptedMessage.length); // Copy encrypted message to the result
        return result;



    }

    private String decryptMessage(byte[] encryptedMessage, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = Arrays.copyOfRange(encryptedMessage, 0, 12); // Extract IV from the beginning of the message
        byte[] cipherText = Arrays.copyOfRange(encryptedMessage, 12, encryptedMessage.length); // Extract encrypted message
        GCMParameterSpec spec = new GCMParameterSpec(128, iv); //128 bit auth tag length
        cipher.init(Cipher.DECRYPT_MODE, key,spec);
        return new String(cipher.doFinal(cipherText));
    }

    private SecretKey deriveKey(byte[] previousKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(previousKey);

        byte[] newKeyBytes = Arrays.copyOf(hash, 16); // Use the first 16 bytes of the hash as the new key
        return new SecretKeySpec(newKeyBytes, "AES");
    }
}
