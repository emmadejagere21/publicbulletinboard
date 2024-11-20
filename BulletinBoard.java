import java.security.MessageDigest;
import java.util.*;

public class BulletinBoard {
    private Map<Integer, List<Pair<byte[], String>>> board;

    public BulletinBoard(int size) {
        board = new HashMap<>();
        for (int i = 0; i < size; i++) {
            board.put(i, new ArrayList<>());
        }
    }

    public void add(int index, byte[] value, String tag) {
        System.out.println("Toevoegen aan index: " + index + ", Tag: " + tag);
        if (!board.containsKey(index)) {
            System.out.println("Index bestaat niet in het bulletin board.");
            return;
        }
        board.get(index).add(new Pair<>(value, tag));
        System.out.println("Bericht toegevoegd aan index: " + index);
    }

    public byte[] get(int index, String preImage) {
        if (!board.containsKey(index)) {
            System.out.println("Ongeldige index: " + index);
            return null;
        }
        String tag = generateHash(preImage);
        System.out.println("Zoeken naar tag: " + tag + " op index: " + index);
        for (Pair<byte[], String> entry : board.get(index)) {
            if (entry.getSecond().equals(tag)) {
                System.out.println("Bericht gevonden en verwijderd op index: " + index);
                board.get(index).remove(entry);
                return entry.getFirst();
            }
        }
        System.out.println("Geen bericht gevonden op index: " + index + " met tag: " + tag);
        return null; // Geen bericht gevonden
    }

    public int size() {
        return board.size();
    }

    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(input.getBytes());
            String hash = bytesToHex(encodedHash);
            System.out.println("Genereren hash voor input: " + input + ", Hash: " + hash);
            return hash;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Debugging method to print the state of the bulletin board
    public void printState() {
        System.out.println("Bulletin Board State:");
        for (Map.Entry<Integer, List<Pair<byte[], String>>> entry : board.entrySet()) {
            System.out.println("Index: " + entry.getKey() + ", Berichten: " + entry.getValue().size());
            for (Pair<byte[], String> pair : entry.getValue()) {
                System.out.println(" - Tag: " + pair.getSecond());
            }
        }
    }
}
