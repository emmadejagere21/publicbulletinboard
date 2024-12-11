import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.rmi.Naming;
import java.security.*;
import java.util.*;

public class Client {
    private int idxAB;
    private String tagAB;
    private SecretKey keyAB;
    private BulletinBoardInterface board;
    private static final Random RANDOM = new SecureRandom();

    private static final int IV_SIZE = 12;        // IV size for AES-GCM
    private static final int TAG_LENGTH = 128;    // Authentication tag length for AES-GCM
    private static final int DUMMY_RATE = 3;      // One dummy message per 3 real messages for unlinkability
    private static final int MAX_MESSAGES = 3;
    private int sentMessages = 0;
    private Queue<Long> messageTimestamps = new LinkedList<>();
    private static final long TIME_WINDOW_MS = 60000;

    private int messageCounter = 0;  // Tracks message count to insert dummies at regular intervals

    public Client(String serverAddress, int initialIndex, String initialTag, SecretKey initialKey) throws Exception {
        this.board = (BulletinBoardInterface) Naming.lookup("rmi://" + serverAddress + "/BulletinBoard");
        this.idxAB = initialIndex;
        this.tagAB = initialTag;
        this.keyAB = initialKey;
    }

    // Send a message with dummy messages injected periodically for unlinkability
    public void send(String message) throws Exception {
        long currentTime = System.currentTimeMillis();

        // Verwijder oude tijdstempels buiten het tijdsvenster
        while (!messageTimestamps.isEmpty() && currentTime - messageTimestamps.peek() > TIME_WINDOW_MS) {
            messageTimestamps.poll();
        }

        // Controleer of de limiet is bereikt
        if (messageTimestamps.size() >= MAX_MESSAGES) {
            throw new Exception("Rate limit exceeded: Maximum " + MAX_MESSAGES + " messages per minute allowed.");
        }

        // Voeg de huidige tijdstempel toe en verstuur het bericht
        messageTimestamps.add(currentTime);

        // Bestaande logica voor het verzenden van berichten
        int nextIndex = RANDOM.nextInt(board.size());
        String nextTag = UUID.randomUUID().toString();
        String payload = message + "||" + nextIndex + "||" + nextTag;

        byte[] encryptedMessage = encryptMessage(payload, keyAB);
        board.add(idxAB, encryptedMessage, tagAB);

        // Injecteer dummy-berichten indien nodig
        if (++messageCounter % DUMMY_RATE == 0) {
            String dummyPayload = generateDummyPayload();
            byte[] encryptedDummy = encryptMessage(dummyPayload, keyAB);
            board.add(RANDOM.nextInt(board.size()), encryptedDummy, UUID.randomUUID().toString());
        }

        idxAB = nextIndex;
        tagAB = nextTag;
        keyAB = deriveKey(keyAB.getEncoded());
    }


    // Generate a dummy payload
    private String generateDummyPayload() {
        byte[] dummyData = new byte[32];
        RANDOM.nextBytes(dummyData);
        return Base64.getEncoder().encodeToString(dummyData);
    }

    // Receive a message
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

    // Encrypt a message using AES-GCM
    private byte[] encryptMessage(String message, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[IV_SIZE];
        RANDOM.nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] encryptedMessage = cipher.doFinal(message.getBytes());
        byte[] result = new byte[iv.length + encryptedMessage.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encryptedMessage, 0, result, iv.length, encryptedMessage.length);
        return result;
    }

    // Decrypt a message using AES-GCM
    private String decryptMessage(byte[] encryptedMessage, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = Arrays.copyOfRange(encryptedMessage, 0, IV_SIZE);
        byte[] cipherText = Arrays.copyOfRange(encryptedMessage, IV_SIZE, encryptedMessage.length);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return new String(cipher.doFinal(cipherText));
    }

    // Derive a new key from the previous key using SHA-256 for forward secrecy
    private SecretKey deriveKey(byte[] previousKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(previousKey);
        byte[] newKeyBytes = Arrays.copyOf(hash, 16); // Use first 16 bytes for AES-128
        return new SecretKeySpec(newKeyBytes, "AES");
    }
}
