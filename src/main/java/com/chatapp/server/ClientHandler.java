package com.chatapp.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ChatServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.clientSocket = socket;
        this.server = server;
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                handleMessage(inputLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void handleMessage(String data) {
        String[] parts = data.split(":", 2);
        if (parts.length != 2) return;

        String command = parts[0];
        String content = parts[1];

        switch (command) {
            case "GET_GROUPS":
                server.sendGroupListToUser(this);
                break;
            case "MESSAGE":
                String[] messageParts = content.split(":", 2);
                if (messageParts.length == 2) {
                    String receiver = messageParts[0];
                    String message = messageParts[1];
                    server.sendPrivateMessage(username, receiver, message);
                }
                break;
            case "GROUP":
                String[] groupParts = content.split(":", 2);
                if (groupParts.length == 2) {
                    String groupId = groupParts[0];
                    String message = groupParts[1];
                    server.sendGroupMessage(groupId, username, message);
                }
                break;
            case "FILE":
                String[] fileParts = content.split(":", 3);
                if (fileParts.length == 3) {
                    String receiver = fileParts[0];
                    String fileName = fileParts[1];
                    String filePath = fileParts[2];
                    server.sendPrivateFile(username, receiver, fileName, filePath);
                }
                break;
            case "GROUP_FILE":
                String[] groupFileParts = content.split(":", 3);
                if (groupFileParts.length == 3) {
                    String groupId = groupFileParts[0];
                    String fileName = groupFileParts[1];
                    String filePath = groupFileParts[2];
                    server.sendGroupFile(groupId, username, fileName, filePath);
                }
                break;
            case "HISTORY":
                server.loadChatHistory(username, content, this);
                break;
            case "GROUP_HISTORY":
                server.loadGroupHistory(content, this);
                break;
            case "CLEAR_HISTORY":
                server.clearChatHistory(username, content);
                break;
            case "CLEAR_GROUP_HISTORY":
                server.clearGroupHistory(content, username);
                break;
            case "DELETE_MESSAGE":
                String[] deleteParts = content.split(":", 2);
                if (deleteParts.length == 2) {
                    String otherUser = deleteParts[0];
                    String message = deleteParts[1];
                    server.deleteMessage(username, otherUser, message);
                }
                break;
            case "DELETE_GROUP_MESSAGE":
                String[] deleteGroupParts = content.split(":", 2);
                if (deleteGroupParts.length == 2) {
                    String groupId = deleteGroupParts[0];
                    String message = deleteGroupParts[1];
                    server.deleteGroupMessage(groupId, username, message);
                }
                break;
            case "LOGIN":
                handleLogin(content);
                break;
            case "REGISTER":
                handleRegister(content);
                break;
            case "CREATE_GROUP":
                handleCreateGroup(content);
                break;
            case "ADD_TO_GROUP":
                handleAddToGroup(content);
                break;
            case "REMOVE_FROM_GROUP":
                handleRemoveFromGroup(content);
                break;
            case "GROUP_MESSAGE":
                handleGroupMessage(content);
                break;
            case "FILE_TRANSFER":
                handleFileTransfer(content);
                break;
            default:
                System.out.println("Unknown command: " + command);
        }
    }

    private void handleLogin(String data) {
        String[] credentials = data.split(":");
        String username = credentials[0];
        String password = credentials[1];

        if (validateLogin(username, password)) {
            this.username = username;
            server.addOnlineUser(username, this);
            sendMessage("LOGIN_SUCCESS");
            server.broadcastUserList();
        } else {
            sendMessage("LOGIN_FAILED");
        }
    }

    private void handleRegister(String data) {
        String[] userData = data.split(":");
        String username = userData[0];
        String password = userData[1];
        String email = userData[2];

        if (registerUser(username, password, email)) {
            sendMessage("REGISTER_SUCCESS");
        } else {
            sendMessage("REGISTER_FAILED");
        }
    }

    private void handlePrivateMessage(String data) {
        String[] messageData = data.split(":", 2);
        String receiver = messageData[0];
        String message = messageData[1];
        server.sendPrivateMessage(username, receiver, message);
    }

    private void handleGroupMessage(String data) {
        String[] parts = data.split(":", 2);
        String groupId = parts[0];
        String message = parts[1];
        server.sendGroupMessage(groupId, username, message);
    }

    private void handleFileTransfer(String data) {
        try {
            String[] parts = data.split(":", 2);
            String target = parts[0];
            String fileName = parts[1];
            
            // Đọc file từ client
            byte[] fileData = new byte[1024 * 1024]; // Buffer 1MB
            int bytesRead = clientSocket.getInputStream().read(fileData);
            
            if (bytesRead > 0) {
                // Tạo thư mục server_files nếu chưa tồn tại
                File serverDir = new File("server_files");
                if (!serverDir.exists()) {
                    serverDir.mkdir();
                }
                
                // Tạo tên file duy nhất bằng cách thêm timestamp
                String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
                String filePath = "server_files/" + uniqueFileName;
                String absoluteFilePath = new File(filePath).getAbsolutePath();
                
                // Lưu file vào thư mục server_files
                FileOutputStream fos = new FileOutputStream(filePath);
                fos.write(fileData, 0, bytesRead);
                fos.close();

                // Đọc end marker
                String endMarker = in.readLine();
                if ("FILE_END".equals(endMarker)) {
                    // Gửi thông báo cho người nhận
                    if (target.startsWith("GROUP_")) {
                        // Gửi file cho group
                        server.sendGroupFile(target, username, fileName, absoluteFilePath);
                    } else {
                        // Gửi file cho user
                        server.sendPrivateFile(username, target, fileName, absoluteFilePath);
                    }
                } else {
                    System.out.println("Error: File transfer did not end properly");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            out.println("ERROR:Failed to transfer file: " + e.getMessage());
        }
    }

    private void handleChatHistory(String data) {
        String[] parts = data.split(":");
        String otherUser = parts[0];
        server.loadChatHistory(username, otherUser, this);
    }

    private void handleGroupHistory(String data) {
        String[] parts = data.split(":");
        String groupId = parts[0];
        server.loadGroupHistory(groupId, this);
    }

    private void handleClearHistory(String data) {
        String otherUser = data;
        server.clearChatHistory(username, otherUser);
    }

    private void handleClearGroupHistory(String data) {
        String groupId = data;
        server.clearGroupHistory(groupId, username);
    }

    private void handleCreateGroup(String data) {
        String[] parts = data.split(":", 2);
        String groupId = parts[0];
        server.createGroup(groupId, username);
    }

    private void handleAddToGroup(String data) {
        String[] parts = data.split(":", 2);
        String groupId = parts[0];
        String newMember = parts[1];
        server.addMemberToGroup(groupId, newMember);
    }

    private void handleRemoveFromGroup(String data) {
        String[] parts = data.split(":", 2);
        String groupId = parts[0];
        String member = parts[1];
        server.removeMemberFromGroup(groupId, member);
    }

    private boolean validateLogin(String username, String password) {
        try {
            CSVReader reader = new CSVReader(new FileReader("users.csv"));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine[0].equals(username) && nextLine[1].equals(password)) {
                    reader.close();
                    return true;
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean registerUser(String username, String password, String email) {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter("users.csv", true));
            String[] record = {username, password, email};
            writer.writeNext(record);
            writer.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private void cleanup() {
        try {
            if (username != null) {
                server.removeOnlineUser(username);
            }
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }
} 