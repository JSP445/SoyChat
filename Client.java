import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.FileWriter;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Client implements Runnable {

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;
    private JFrame frame;
    private JTextArea textArea;
    private JTextField textField;
    private String nickname;
    private static final Map<String, String> EMOJI_MAP = new HashMap<>();

    static {
        EMOJI_MAP.put(":smile:", "\uD83D\uDE04");
        EMOJI_MAP.put(":heart:", "\u2764\uFE0F");
        EMOJI_MAP.put(":thumbsup:", "\uD83D\uDC4D");
        // Add more emojis as needed
    }

    @Override
    public void run() {
        try {
            client = new Socket("127.0.0.1", 9999);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            // Send the nickname to the server first thing
            out.println(nickname + " joined the chat!");

            String inMessage;
            while ((inMessage = in.readLine()) != null) {
                final String message = inMessage;
                SwingUtilities.invokeLater(() -> {
                    textArea.append(message + "\n");
                });
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    public void shutdown() {
        done = true;
        try {
            in.close();
            out.close();
            if (!client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    public Client() {
        frame = new JFrame("SoyChat");
        textArea = new JTextArea(20, 50);
        textArea.setEditable(false);
        textField = new JTextField(50);

        // Load and set the icon for the window
        ImageIcon icon = new ImageIcon(getClass().getResource("./emotes/soyjak.jpg"));
        frame.setIconImage(icon.getImage());

        // Prompt the user for a nickname once
        nickname = JOptionPane.showInputDialog(frame, "Enter your nickname:", "Nickname", JOptionPane.PLAIN_MESSAGE);
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = "Anonymous";
        }

        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String message = textField.getText().trim();
                    if (!message.isEmpty()) {
                        message = replaceEmojis(message);
                        if (message.startsWith("/msg ")) {
                            handlePrivateMessage(message);
                        } else if (message.equals("/clear")) {
                            textArea.setText("");
                        } else if (message.equals("/save")) {
                            saveChatHistory();
                        } else {
                            out.println(nickname + ": " + message);
                        }
                        textField.setText("");
                        if (message.equals("/quit")) {
                            shutdown();
                        }
                    }
                }
            }
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER);
        frame.add(textField, BorderLayout.SOUTH);

        frame.pack();
        frame.setVisible(true);
    }

    private String replaceEmojis(String message) {
        for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        return message;
    }

    private void handlePrivateMessage(String message) {
        out.println(message);  // This message should be parsed and handled on the server side for proper delivery.
    }

    private void saveChatHistory() {
        try (FileWriter writer = new FileWriter(new File("chat_history.txt"))) {
            writer.write(textArea.getText());
            JOptionPane.showMessageDialog(frame, "Chat history saved!", "Save Chat", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Failed to save chat history.", "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            new Thread(client).start();
        });
    }
}
