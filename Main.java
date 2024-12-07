import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.rmi.Naming;
import java.security.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class Main {
    private static Client alice;
    private static Client bob;
    private static JTextArea aliceMessages;
    private static JTextArea bobMessages;
    private static JLabel aliceStatus;
    private static JLabel bobStatus;
    private static JButton aliceToggleStatus;
    private static JButton bobToggleStatus;
    private static final String MESSAGES_DIR = "messages";

    public static void main(String[] args) throws Exception {
        // Diffie-Hellman key exchange for local clients
        KeyPair aliceKeyPair = generateDHKeyPair();
        KeyPair bobKeyPair = generateDHKeyPair();

        // Exchange public keys and derive shared secrets
        SecretKey aliceSharedKey = deriveSharedSecret(aliceKeyPair.getPrivate(), bobKeyPair.getPublic());
        SecretKey bobSharedKey = deriveSharedSecret(bobKeyPair.getPrivate(), aliceKeyPair.getPublic());

        // Verify that both shared keys are the same
        assert aliceSharedKey.equals(bobSharedKey) : "Key mismatch between Alice and Bob";

        // Shared initial tag for both clients
        String sharedInitialTag = "sharedInitialTag";

        // Create two clients using the derived shared key
        alice = new Client("localhost", 0, sharedInitialTag, aliceSharedKey);
        bob = new Client("localhost", 0, sharedInitialTag, bobSharedKey);

        // Set up the GUI
        JFrame frame = new JFrame("Secure Messaging App with RMI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Alice's Panel
        aliceMessages = new JTextArea();
        aliceMessages.setEditable(false);
        aliceStatus = new JLabel("Status: Offline");
        aliceToggleStatus = new JButton("Go Online");
        JScrollPane aliceScroll = new JScrollPane(aliceMessages);
        JTextField aliceInput = new JTextField();
        JButton aliceSend = new JButton("Send");
        JPanel alicePanel = createUserPanel("Alice", Color.BLUE, aliceMessages, aliceInput, aliceSend, aliceStatus, aliceToggleStatus);

        // Bob's Panel
        bobMessages = new JTextArea();
        bobMessages.setEditable(false);
        bobStatus = new JLabel("Status: Offline");
        bobToggleStatus = new JButton("Go Online");
        JScrollPane bobScroll = new JScrollPane(bobMessages);
        JTextField bobInput = new JTextField();
        JButton bobSend = new JButton("Send");
        JPanel bobPanel = createUserPanel("Bob", Color.GREEN, bobMessages, bobInput, bobSend, bobStatus, bobToggleStatus);

        // Add panels to the main layout
        mainPanel.add(alicePanel);
        mainPanel.add(bobPanel);
        frame.add(mainPanel);

        // Add export button
        JButton exportButton = new JButton("Export Conversation");
        exportButton.addActionListener(e -> exportConversation(frame));
        frame.add(exportButton, BorderLayout.SOUTH);

        frame.setVisible(true);

        // Action listeners for toggle status buttons
        aliceToggleStatus.addActionListener(e ->
                toggleClientStatus(alice, aliceStatus, aliceToggleStatus, aliceMessages, "Alice"));

        bobToggleStatus.addActionListener(e ->
                toggleClientStatus(bob, bobStatus, bobToggleStatus, bobMessages, "Bob"));

        aliceSend.addActionListener(e -> {
            String message = aliceInput.getText();
            if (!message.isEmpty()) {
                try {
                    if (!alice.isOnline()) {
                        showError(frame, "Alice is offline. Cannot send messages.");
                        return;
                    }
                    alice.send(message);
                    aliceMessages.append("Alice: " + message + "\n");
                    aliceInput.setText("");
                } catch (Exception ex) {
                    showError(frame, "Failed to send message: " + ex.getMessage());
                }
            }
        });

        bobSend.addActionListener(e -> {
            String message = bobInput.getText();
            if (!message.isEmpty()) {
                try {
                    if (!bob.isOnline()) {
                        showError(frame, "Bob is offline. Cannot send messages.");
                        return;
                    }
                    bob.send(message);
                    bobMessages.append("Bob: " + message + "\n");
                    bobInput.setText("");
                } catch (Exception ex) {
                    showError(frame, "Failed to send message: " + ex.getMessage());
                }
            }
        });


        // Automatic status and message checking
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (alice.isOnline()) {
                            String aliceMessage = alice.receive();
                            if (aliceMessage != null) {
                                aliceMessages.append("Bob: " + aliceMessage + "\n");
                                System.out.println("Bericht ontvangen door Alice: " + aliceMessage);
                            }
                        }
                        if (bob.isOnline()) {
                            String bobMessage = bob.receive();
                            if (bobMessage != null) {
                                bobMessages.append("Alice: " + bobMessage + "\n");
                                System.out.println("Bericht ontvangen door Bob: " + bobMessage);
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }, 1000, 1000); // Controleer elke seconde

        // Elke seconde controleren



    }


    private static JPanel createUserPanel(String username, Color borderColor, JTextArea messageArea, JTextField inputField, JButton sendButton, JLabel statusLabel, JButton toggleStatusButton) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(borderColor, 2), username));
        panel.setBackground(new Color(240, 240, 255));

        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        JPanel statusPanel = new JPanel(new GridLayout(1, 2));
        statusPanel.add(statusLabel);
        statusPanel.add(toggleStatusButton);

        panel.add(statusPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    private static void toggleClientStatus(Client client, JLabel statusLabel, JButton toggleButton, JTextArea messageArea, String clientName) {
        try {
            if (client.isOnline()) {
                client.logout();
                statusLabel.setText("Status: Offline");
                toggleButton.setText("Go Online");
                System.out.println(clientName + " is offline.");
            } else {
                client.login();
                statusLabel.setText("Status: Online");
                toggleButton.setText("Go Offline");
                System.out.println(clientName + " is online.");

                // Offline berichten ophalen en aan de GUI toevoegen
                List<byte[]> offlineMessages = client.receiveOfflineMessages();
                for (byte[] encryptedMessage : offlineMessages) {
                    String message = client.decryptMessage(encryptedMessage);
                    messageArea.append(clientName + ": " + message + "\n");
                    System.out.println("Offline bericht toegevoegd aan GUI: " + message);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Fout bij status wisselen: " + e.getMessage(), "Fout", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private static void showError(JFrame frame, String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static KeyPair generateDHKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    private static SecretKey deriveSharedSecret(PrivateKey privateKey, PublicKey publicKey) throws Exception {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);
        byte[] sharedSecret = keyAgreement.generateSecret();
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] derivedKey = Arrays.copyOf(sha256.digest(sharedSecret), 16); // AES-128 key size
        return new SecretKeySpec(derivedKey, "AES");
    }

    private static void exportConversation(JFrame frame) {
        File messagesDir = new File(MESSAGES_DIR);
        if (!messagesDir.exists()) {
            messagesDir.mkdir();
        }

        String fileName = JOptionPane.showInputDialog(frame, "Enter file name to save:", "Export Conversation", JOptionPane.PLAIN_MESSAGE);
        if (fileName != null && !fileName.trim().isEmpty()) {
            File fileToSave = new File(messagesDir, fileName + ".json");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                Gson gson = new Gson();
                List<String> messages = Arrays.asList(aliceMessages.getText(), bobMessages.getText());
                writer.write(gson.toJson(messages));
            } catch (IOException e) {
                showError(frame, "Failed to save conversation: " + e.getMessage());
            }
        }
    }

    private static void importConversation(JFrame frame) {
        JFileChooser fileChooser = new JFileChooser(MESSAGES_DIR);
        fileChooser.setDialogTitle("Import Conversation");
        int userSelection = fileChooser.showOpenDialog(frame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(fileToOpen))) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<String>>() {}.getType();
                List<String> messages = gson.fromJson(reader, type);
                if (messages.size() == 2) {
                    aliceMessages.setText(messages.get(0));
                    bobMessages.setText(messages.get(1));
                }
            } catch (IOException e) {
                showError(frame, "Failed to load conversation: " + e.getMessage());
            }
        }
    }
}