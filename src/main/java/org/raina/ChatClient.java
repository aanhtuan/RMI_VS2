package org.raina;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ChatClient extends UnicastRemoteObject implements ChatClientCallback {
    private static final long serialVersionUID = 1L;

    private transient JFrame frame;
    private transient JTextArea textArea;
    private transient JTextField inputField;
    private transient JButton sendBtn;
    private transient JButton disconnectBtn;
    private ChatServer server;
    private String username;

    protected ChatClient() throws RemoteException {
        super();
        initGui();
    }

    private void initGui() {
        frame = new JFrame("RMI Chat");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(textArea);
        frame.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendBtn = new JButton("Send");
        disconnectBtn = new JButton("Disconnect");

        bottom.add(inputField, BorderLayout.CENTER);
        JPanel right = new JPanel(new GridLayout(1, 2));
        right.add(sendBtn);
        right.add(disconnectBtn);
        bottom.add(right, BorderLayout.EAST);

        frame.add(bottom, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        disconnectBtn.addActionListener(e -> disconnect());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (server != null && username != null) server.unregister(username);
                    UnicastRemoteObject.unexportObject(ChatClient.this, true);
                } catch (Exception ex) {
                    // ignore
                }
                System.exit(0);
            }
        });

        frame.setVisible(true);

        // show connect dialog on EDT
        SwingUtilities.invokeLater(this::showConnectDialog);
    }

    private void showConnectDialog() {
        JTextField hostField = new JTextField("192.168.1.175");
        JTextField portField = new JTextField("1099");
        JTextField nameField = new JTextField(System.getProperty("user.name", "user"));
        Object[] message = {
                "Server host:", hostField,
                "Registry port:", portField,
                "Your name:", nameField
        };

        int option = JOptionPane.showConfirmDialog(frame, message, "Connect to ChatServer", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) {
            frame.dispose();
            System.exit(0);
            return;
        }

        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(frame, "Invalid port number. Using 1099.");
            port = 1099;
        }
        username = nameField.getText().trim();
        if (username.isEmpty()) username = "user";

        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            server = (ChatServer) registry.lookup("ChatServer");
            server.register(username, this);
            appendText("<system>", "Connected to server at " + host + ":" + port + " as '" + username + "'");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Failed to connect: " + ex.getMessage());
            ex.printStackTrace();
            frame.dispose();
            System.exit(1);
        }
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;
        try {
            server.sendMessage(username, msg);
            inputField.setText("");
        } catch (RemoteException e) {
            appendText("<system>", "Failed to send message: " + e.getMessage());
        } catch (NullPointerException npe) {
            appendText("<system>", "Not connected to server.");
        }
    }

    private void disconnect() {
        try {
            if (server != null && username != null) server.unregister(username);
        } catch (RemoteException e) {
            // ignore
        }
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (Exception ignored) {
        }
        appendText("<system>", "Disconnected");
    }

    @Override
    public void deliver(String from, String message) throws RemoteException {
        appendText(from, message);
    }

    private void appendText(String from, String message) {
        SwingUtilities.invokeLater(() -> {
            textArea.append("[" + from + "] " + message + "\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        // ensure GUI runs on EDT
        SwingUtilities.invokeLater(() -> {
            try {
                new ChatClient();
            } catch (RemoteException e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}
