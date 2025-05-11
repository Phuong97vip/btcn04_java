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
import javax.swing.JOptionPane;

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
            case "GROUP_ADDED":
                handleGroupAdded(data);
                break;
            case "GROUP_MEMBER_ADDED":
                handleGroupMemberAdded(data);
                break;
            case "GROUP_MEMBERS":
                handleGroupMembers(data);
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
        
        // Request group list after successful login
        sendMessage("GET_GROUPS");
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
                if (parts.length > 0) {
                    String groupId = parts[0];
                    if (!groupListModel.contains(groupId)) {
                        groupListModel.addElement(groupId);
                    }
                }
            }
        }
        if (chatWindow != null) {
            chatWindow.updateGroupList(groupListModel);
            // Request chat history for each group
            for (int i = 0; i < groupListModel.size(); i++) {
                String groupId = groupListModel.get(i);
                sendMessage("GROUP_HISTORY:" + groupId);
            }
        }
    }

    private void handleGroupAdded(String groupId) {
        if (chatWindow != null) {
            chatWindow.addGroup(groupId);
        }
    }

    private void handleGroupMemberAdded(String data) {
        String[] parts = data.split(":");
        String groupId = parts[0];
        String newMember = parts[1];
        if (chatWindow != null) {
            chatWindow.addGroupMember(groupId, newMember);
        }
    }

    private void handleGroupMembers(String data) {
        String[] parts = data.split(":", 2);
        String groupId = parts[0];
        String[] members = parts[1].split(",");
        if (chatWindow != null) {
            chatWindow.updateGroupMembers(groupId, members);
        }
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
            // Send file metadata first
            String command = isGroup ? "GROUP_FILE:" : "FILE:";
            out.println(command + target + ":" + file.getName());
            out.flush(); // Ensure metadata is sent before file content

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

            // Send end marker
            out.println("FILE_END");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Error sending file: " + e.getMessage(),
                "Error", 
                JOptionPane.ERROR_MESSAGE);
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