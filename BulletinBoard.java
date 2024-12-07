import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.util.*;

public class BulletinBoard extends UnicastRemoteObject implements BulletinBoardInterface {
    private Map<Integer, List<Message>> board;
    private Map<String, Boolean> clientStatus = new HashMap<>();
    private Map<String, Queue<byte[]>> offlineMessages = new HashMap<>();

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
        String hashedTag = generateHash(tag);
        board.computeIfAbsent(index, k -> new ArrayList<>());
        board.get(index).add(new Message(value, hashedTag));
        System.out.println("Bericht toegevoegd aan index: " + index + " met tag: " + tag);
    }




    @Override
    public synchronized byte[] get(int index, String preImage) throws RemoteException {
        if (!board.containsKey(index)) {
            System.out.println("Ongeldige index: " + index);
            return null;
        }

        String tag = generateHash(preImage);
        for (Message entry : board.get(index)) {
            if (entry.tag.equals(tag) && !entry.deleted) {
                entry.markAsDeleted();
                System.out.println("Bericht opgehaald van index: " + index + " met tag: " + tag);
                return entry.value;
            }
        }
        System.out.println("Geen bericht gevonden voor index: " + index + ", tag: " + tag);
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

    @Override
    public synchronized void updateClientStatus(String tag, boolean status) throws RemoteException {
        clientStatus.put(tag, status);
        System.out.println("Status bijgewerkt: " + tag + " is " + (status ? "online" : "offline"));
    }

    @Override
    public synchronized boolean isClientOnline(String tag) throws RemoteException {
        return clientStatus.getOrDefault(tag, false);
    }

    // Add method to store offline messages
    public synchronized void saveOfflineMessage(String tag, byte[] message) throws RemoteException {
        offlineMessages.putIfAbsent(tag, new LinkedList<>());
        offlineMessages.get(tag).add(message);
        System.out.println("Message saved for offline user with tag: " + tag);
    }

    // Add method to retrieve offline messages
    @Override
    public synchronized List<byte[]> getOfflineMessages(String tag) throws RemoteException {
        List<byte[]> messages = new ArrayList<>(offlineMessages.getOrDefault(tag, new LinkedList<>()));
        offlineMessages.remove(tag); // Clear messages after retrieval
        return messages;
    }



}
