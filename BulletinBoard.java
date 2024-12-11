import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.util.*;

public class BulletinBoard extends UnicastRemoteObject implements BulletinBoardInterface {
    private Map<Integer, List<Message>> board;
    private Map<String, Integer> userMessageCounts = new HashMap<>();
    private static final int MAX_MESSAGES_PER_USER = 100;

    // Define Message class to store value, tag, and deleted flag
    static class Message {
        byte[] value;
        String tag;
        boolean deleted;

        Message(byte[] value, String tag) {
            this.value = value;
            this.tag = tag;
            this.deleted = false;  // Initially not deleted
        }

        void markAsDeleted() {
            this.deleted = true;
        }
    }

    public BulletinBoard(int size) throws RemoteException {
        super();
        board = new HashMap<>();
        for (int i = 0; i < size; i++) {
            board.put(i, new ArrayList<>());
        }
    }

    @Override
    public synchronized void add(int index, byte[] value, String tag) throws RemoteException {
        System.out.println("Adding at index: " + index + ", Tag: " + tag);
        if (!board.containsKey(index)) {
            System.out.println("Index does not exist.");
            return;
        }

        String hashedTag = generateHash(tag);

        // Append the new message instead of deleting previous ones
        board.get(index).add(new Message(value, hashedTag));
    }

    @Override
    public synchronized byte[] get(int index, String preImage) throws RemoteException {
        if (!board.containsKey(index)) {
            System.out.println("Invalid index: " + index);
            return null;
        }

        String tag = generateHash(preImage);
        for (Message entry : board.get(index)) {
            if (entry.tag.equals(tag) && !entry.deleted) {
                // Instead of deleting, we mark as deleted
                entry.markAsDeleted();
                return entry.value;
            }
        }
        return null;
    }

    @Override
    public int size() throws RemoteException {
        return board.size();
    }

    // Helper method to generate hash of the tag
    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(input.getBytes());
            return bytesToHex(encodedHash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
