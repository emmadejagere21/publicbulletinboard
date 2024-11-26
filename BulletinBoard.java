import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.util.*;

public class BulletinBoard extends UnicastRemoteObject implements BulletinBoardInterface {
    private Map<Integer, List<Pair<byte[], String>>> board;

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

        //Avoiding duplicates
        for (Pair<byte[], String> entry : board.get(index)) {
            if (entry.getSecond().equals(hashedTag)) {
                System.out.println("Tag already exists.");
                return;
            }
        }

        board.get(index).add(new Pair<>(value, hashedTag));
    }

    @Override
    public synchronized byte[] get(int index, String preImage) throws RemoteException {
        if (!board.containsKey(index)) {
            System.out.println("Invalid index: " + index);
            return null;
        }
        String tag = generateHash(preImage);
        for (Pair<byte[], String> entry : board.get(index)) {
            if (entry.getSecond().equals(tag)) {
                board.get(index).remove(entry);
                return entry.getFirst();
            }
        }
        return null;
    }

    @Override
    public int size() throws RemoteException {
        return board.size();
    }

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
