package com.chatapp.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.DefaultListModel;

public class ChatClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private LoginWindow loginWindow;
    private ChatWindow chatWindow;
    private String username;
    private ExecutorService messageListener;

    public ChatClient() {
        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            startMessageListener();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void startMessageListener() {
        messageListener = Executors.newSingleThreadExecutor();
        messageListener.execute(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    handleServerMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleServerMessage(String message) {
        String[] parts = message.split(":", 2);
        String command = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "LOGIN_SUCCESS":
                handleLoginSuccess();
                break;
            case "LOGIN_FAILED":
                loginWindow.showError("Login failed. Please check your credentials.");
                break;
            case "REGISTER_SUCCESS":
                loginWindow.showSuccess("Registration successful. Please login.");
                break;
            case "REGISTER_FAILED":
                loginWindow.showError("Registration failed. Username might be taken.");
                break;
            case "USERS":
                handleUserList(data);
                break;
            case "GROUP_LIST":
                handleGroupList(data);
                break;
            case "MESSAGE":
                handlePrivateMessage(data);
                break;
            case "GROUP":
                handleGroupMessage(data);
                break;
            case "FILE":
                handleFileTransfer(data);
                break;
            case "HISTORY":
                handleChatHistory(data);
                break;
            case "GROUP_HISTORY":
                handleGroupHistory(data);
                break;
            default:
                System.out.println("Unknown command: " + command);
        }
    }

    private void handleLoginSuccess() {
        username = loginWindow.getUsername();
        loginWindow.dispose();
        chatWindow = new ChatWindow(this, username);
        chatWindow.setVisible(true);
    }

    private void handleUserList(String data) {
        String[] users = data.split(",");
        updateUserList(users);
    }

    private void handleGroupList(String data) {
        String[] groups = data.split(";");
        DefaultListModel<String> groupListModel = new DefaultListModel<>();
        for (String group : groups) {
            if (!group.isEmpty()) {
                String[] parts = group.split(":");
                groupListModel.addElement(parts[0]);
            }
        }
        chatWindow.updateGroupList(groupListModel);
    }

    private void handlePrivateMessage(String data) {
        String[] messageData = data.split(":", 2);
        String sender = messageData[0];
        String message = messageData[1];
        if (chatWindow != null) {
            chatWindow.receiveMessage(sender, message);
        }
    }

    private void handleGroupMessage(String data) {
        String[] parts = data.split(":", 3);
        String groupId = parts[0];
        String sender = parts[1];
        String message = parts[2];
        chatWindow.addGroupMessage(groupId, sender, message);
    }

    private void handleFileTransfer(String data) {
        String[] parts = data.split(":", 3);
        String sender = parts[0];
        String fileName = parts[1];
        String filePath = parts[2];
        
        if (chatWindow != null) {
            chatWindow.receiveFile(sender, fileName, filePath);
        }
    }

    private void handleChatHistory(String data) {
        String[] messageData = data.split(":", 2);
        String sender = messageData[0];
        String message = messageData[1];
        if (chatWindow != null) {
            chatWindow.receiveMessage(sender, message);
        }
    }

    private void handleGroupHistory(String data) {
        String[] parts = data.split(":", 3);
        String groupId = parts[0];
        String sender = parts[1];
        String message = parts[2];
        chatWindow.addGroupMessage(groupId, sender, message);
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void sendFile(String target, File file, boolean isGroup) {
        try {
            // Send file metadata
            String command = isGroup ? "GROUP_FILE:" : "PRIVATE_FILE:";
            out.println(command + target + ":" + file.getName());

            // Send file content
            FileInputStream fis = new FileInputStream(file);
            OutputStream os = socket.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setLoginWindow(LoginWindow loginWindow) {
        this.loginWindow = loginWindow;
    }

    public void updateUserList(String[] users) {
        if (chatWindow != null) {
            chatWindow.updateUserList(users);
        }
    }

    public Set<String> getGroupMembers(String groupId) {
        // TODO: Implement this method to get group members from the server
        return new HashSet<>();
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        LoginWindow loginWindow = new LoginWindow(client);
        client.setLoginWindow(loginWindow);
        loginWindow.setVisible(true);
    }
} 