import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.swing.*;
import java.awt.*;
import java.rmi.Naming;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    private static Client alice;
    private static Client bob;
    private static JTextArea aliceMessages;
    private static JTextArea bobMessages;

    public static void main(String[] args) throws Exception {
        // Connect to the remote bulletin board
        String serverAddress = "localhost"; // Change this to the RMI server's address if needed
        BulletinBoardInterface board = (BulletinBoardInterface) Naming.lookup("rmi://" + serverAddress + "/BulletinBoard");

        // Generate an initial key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey initialKey = keyGen.generateKey();

        // Shared tag for Alice and Bob
        String sharedInitialTag = "sharedInitialTag";

        // Create two clients
        alice = new Client(serverAddress, 0, sharedInitialTag, initialKey);
        bob = new Client(serverAddress, 0, sharedInitialTag, initialKey);

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
        JScrollPane aliceScroll = new JScrollPane(aliceMessages);
        JTextField aliceInput = new JTextField();
        JButton aliceSend = new JButton("Send");
        JPanel alicePanel = createUserPanel("Alice", Color.BLUE, aliceMessages, aliceInput, aliceSend);

        // Bob's Panel
        bobMessages = new JTextArea();
        bobMessages.setEditable(false);
        JScrollPane bobScroll = new JScrollPane(bobMessages);
        JTextField bobInput = new JTextField();
        JButton bobSend = new JButton("Send");
        JPanel bobPanel = createUserPanel("Bob", Color.GREEN, bobMessages, bobInput, bobSend);

        // Add panels to the main layout
        mainPanel.add(alicePanel);
        mainPanel.add(bobPanel);

        frame.add(mainPanel);
        frame.setVisible(true);

        // Action listeners for sending messages
        aliceSend.addActionListener(e -> {
            String message = aliceInput.getText();
            if (!message.isEmpty()) {
                try {
                    alice.send(message);
                    aliceMessages.append("Me: " + message + "\n");
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
                    bob.send(message);
                    bobMessages.append("Me: " + message + "\n");
                    bobInput.setText("");
                } catch (Exception ex) {
                    showError(frame, "Failed to send message: " + ex.getMessage());
                }
            }
        });

        // Automatic message checking
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    try {
                        // Bob receives messages from Alice
                        String bobMessage = bob.receive();
                        if (bobMessage != null) {
                            bobMessages.append("Alice: " + bobMessage + "\n");
                        }

                        // Alice receives messages from Bob
                        String aliceMessage = alice.receive();
                        if (aliceMessage != null) {
                            aliceMessages.append("Bob: " + aliceMessage + "\n");
                        }
                    } catch (Exception ex) {
                        // Ignore errors during automatic checking
                    }
                });
            }
        }, 1000, 1000); // Check every second
    }

    private static JPanel createUserPanel(String username, Color borderColor, JTextArea messageArea, JTextField inputField, JButton sendButton) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(borderColor, 2), username));
        panel.setBackground(new Color(240, 240, 255));

        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    private static void showError(JFrame frame, String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
