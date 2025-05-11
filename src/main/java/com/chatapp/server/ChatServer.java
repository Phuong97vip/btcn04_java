package com.chatapp.server;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

public class ChatServer {
    private static final int PORT = 5000;
    private static final String USERS_FILE = "users.csv";
    private static final String MESSAGES_FILE = "messages.csv";
    private static final String GROUP_MESSAGES_FILE = "group_messages.csv";
    private static final String GROUPS_FILE = "groups.csv";
    private static final String SERVER_FILES_DIR = "server_files";

    private ServerSocket serverSocket;
    private Map<String, ClientHandler> onlineUsers;
    private Map<String, Set<String>> groupMembers; // groupId -> Set of usernames
    private ExecutorService threadPool;
    private ReentrantLock fileLock;
    private Map<String, Set<String>> sentMessages = new ConcurrentHashMap<>();
    private Map<String, Long> lastMessageTime = new ConcurrentHashMap<>();

    public ChatServer() {
        onlineUsers = new ConcurrentHashMap<>();
        groupMembers = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();
        fileLock = new ReentrantLock();
        createRequiredFiles();
        loadGroups();
    }

    private void createRequiredFiles() {
        try {
            // Create files if they don't exist
            File usersFile = new File(USERS_FILE);
            File messagesFile = new File(MESSAGES_FILE);
            File groupMessagesFile = new File(GROUP_MESSAGES_FILE);
            File groupsFile = new File(GROUPS_FILE);
            File serverFilesDir = new File(SERVER_FILES_DIR);
            
            if (!usersFile.exists()) {
                usersFile.createNewFile();
                // Add header line to users.csv
                CSVWriter writer = new CSVWriter(new FileWriter(usersFile));
                writer.writeNext(new String[]{"username", "password"});
                writer.close();
            }
            
            if (!messagesFile.exists()) {
                messagesFile.createNewFile();
                // Add header line to messages.csv
                CSVWriter writer = new CSVWriter(new FileWriter(messagesFile));
                writer.writeNext(new String[]{"sender", "receiver", "message", "timestamp", "deleted_by"});
                writer.close();
            }
            
            if (!groupMessagesFile.exists()) {
                groupMessagesFile.createNewFile();
                // Add header line to group_messages.csv
                CSVWriter writer = new CSVWriter(new FileWriter(groupMessagesFile));
                writer.writeNext(new String[]{"group_id", "sender", "message", "timestamp", "deleted_by"});
                writer.close();
            }
            
            if (!groupsFile.exists()) {
                groupsFile.createNewFile();
                // Add header line to groups.csv
                CSVWriter writer = new CSVWriter(new FileWriter(groupsFile));
                writer.writeNext(new String[]{"group_id", "members"});
                writer.close();
            }
            
            if (!serverFilesDir.exists()) {
                serverFilesDir.mkdir();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void loadGroups() {
        fileLock.lock();
        try {
            // Load từ GROUPS_FILE
            File groupsFile = new File(GROUPS_FILE);
            if (groupsFile.exists() && groupsFile.length() > 0) {
                CSVReader reader = new CSVReader(new FileReader(GROUPS_FILE));
                String[] record;
                while ((record = reader.readNext()) != null) {
                    if (record.length >= 2) {
                        String groupId = record[0];
                        String[] members = record[1].split(",");
                        groupMembers.put(groupId, new HashSet<>(Arrays.asList(members)));
                    }
                }
                reader.close();
            }

            // Load từ GROUP_MESSAGES_FILE để đảm bảo không bỏ sót group nào
            File messagesFile = new File(GROUP_MESSAGES_FILE);
            if (messagesFile.exists() && messagesFile.length() > 0) {
                CSVReader reader = new CSVReader(new FileReader(GROUP_MESSAGES_FILE));
                String[] record;
                while ((record = reader.readNext()) != null) {
                    if (record.length >= 2) {
                        String groupId = record[0];
                        String sender = record[1];
                        
                        // Thêm group và sender vào groupMembers nếu chưa tồn tại
                        Set<String> members = groupMembers.computeIfAbsent(groupId, k -> new HashSet<>());
                        members.add(sender);
                    }
                }
                reader.close();
            }

            // Cập nhật lại GROUPS_FILE với thông tin mới nhất
            CSVWriter writer = new CSVWriter(new FileWriter(GROUPS_FILE, false));
            for (Map.Entry<String, Set<String>> entry : groupMembers.entrySet()) {
                String[] record = {entry.getKey(), String.join(",", entry.getValue())};
                writer.writeNext(record);
            }
            writer.close();

        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        } finally {
            fileLock.unlock();
        }
    }

    private void saveGroup(String groupId, Set<String> members) {
        fileLock.lock();
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(GROUPS_FILE, true));
            String[] record = {groupId, String.join(",", members)};
            writer.writeNext(record);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fileLock.unlock();
        }
    }

    public void createGroup(String groupId, String creator) {
        Set<String> members = new HashSet<>();
        members.add(creator);
        groupMembers.put(groupId, members);
        saveGroup(groupId, members);
        
        // Thông báo cho creator về group mới
        ClientHandler creatorClient = onlineUsers.get(creator);
        if (creatorClient != null) {
            creatorClient.sendMessage("GROUP_CREATED:" + groupId);
        }
        
        broadcastGroupList();
    }

    public void addMemberToGroup(String groupId, String username) {
        fileLock.lock();
        try {
            Set<String> members = groupMembers.get(groupId);
            if (members != null) {
                members.add(username);
                updateGroupFile(groupId, members);
                broadcastGroupList();
                
                // Thông báo cho user được thêm vào group
                ClientHandler newMemberClient = onlineUsers.get(username);
                if (newMemberClient != null) {
                    newMemberClient.sendMessage("GROUP_ADDED:" + groupId);
                    // Gửi danh sách thành viên cho thành viên mới
                    handleGetGroupMembers(groupId, newMemberClient);
                }
                
                // Thông báo cho các thành viên khác
                for (String member : members) {
                    if (!member.equals(username)) {
                        ClientHandler memberClient = onlineUsers.get(member);
                        if (memberClient != null) {
                            memberClient.sendMessage("GROUP_MEMBER_ADDED:" + groupId + ":" + username);
                            // Cập nhật danh sách thành viên cho tất cả
                            handleGetGroupMembers(groupId, memberClient);
                        }
                    }
                }
            }
        } finally {
            fileLock.unlock();
        }
    }

    public void removeMemberFromGroup(String groupId, String username) {
        Set<String> members = groupMembers.get(groupId);
        if (members != null) {
            members.remove(username);
            if (members.isEmpty()) {
                groupMembers.remove(groupId);
            }
            updateGroupFile(groupId, members);
            broadcastGroupList();
        }
    }

    private void updateGroupFile(String groupId, Set<String> members) {
        fileLock.lock();
        try {
            // Đọc tất cả groups
            CSVReader reader = new CSVReader(new FileReader(GROUPS_FILE));
            List<String[]> allGroups = new ArrayList<>();
            String[] record;
            while ((record = reader.readNext()) != null) {
                if (!record[0].equals(groupId)) {
                    allGroups.add(record);
                }
            }
            reader.close();

            // Ghi lại file với group đã cập nhật
            CSVWriter writer = new CSVWriter(new FileWriter(GROUPS_FILE, false));
            for (String[] group : allGroups) {
                writer.writeNext(group);
            }
            if (!members.isEmpty()) {
                writer.writeNext(new String[]{groupId, String.join(",", members)});
            }
            writer.close();
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        } finally {
            fileLock.unlock();
        }
    }

    public void broadcastGroupList() {
        for (ClientHandler client : onlineUsers.values()) {
            String username = client.getUsername();
            // Chỉ gửi danh sách group mà user là thành viên
            StringBuilder userGroups = new StringBuilder();
            for (Map.Entry<String, Set<String>> entry : groupMembers.entrySet()) {
                if (entry.getValue().contains(username)) {
                    if (userGroups.length() > 0) {
                        userGroups.append(";");
                    }
                    userGroups.append(entry.getKey()).append(":").append(String.join(",", entry.getValue()));
                }
            }
            client.sendMessage("GROUP_LIST:" + userGroups.toString());
        }
    }

    public void sendGroupMessage(String groupId, String sender, String message) {
        Set<String> members = groupMembers.get(groupId);
        if (members != null) {
            // Tạo message ID duy nhất với timestamp
            long currentTime = System.currentTimeMillis();
            String messageId = groupId + ":" + sender + ":" + message;
            
            // Kiểm tra thời gian giữa các tin nhắn
            Long lastTime = lastMessageTime.get(messageId);
            if (lastTime != null && currentTime - lastTime < 1000) { // Nếu tin nhắn được gửi trong vòng 1 giây
                return; // Bỏ qua tin nhắn trùng lặp
            }
            
            // Cập nhật thời gian gửi tin nhắn
            lastMessageTime.put(messageId, currentTime);
            
            // Lưu tin nhắn vào CSV
            saveGroupMessage(groupId, sender, message);
            
            // Gửi tin nhắn cho tất cả thành viên trong group
            for (String member : members) {
                ClientHandler memberClient = onlineUsers.get(member);
                if (memberClient != null) {
                    // Nếu là người gửi, gửi với prefix "YOU"
                    if (member.equals(sender)) {
                        memberClient.sendMessage("GROUP:" + groupId + ":YOU:" + message);
                    } else {
                        // Nếu là người nhận khác, gửi với tên người gửi
                        memberClient.sendMessage("GROUP:" + groupId + ":" + sender + ":" + message);
                    }
                }
            }
        }
    }

    public Set<String> getGroupMembers(String groupId) {
        return groupMembers.getOrDefault(groupId, new HashSet<>());
    }

    public boolean isGroupMember(String groupId, String username) {
        Set<String> members = groupMembers.get(groupId);
        return members != null && members.contains(username);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastUserList() {
        String userList = String.join(",", onlineUsers.keySet());
        System.out.println("Broadcasting user list: " + userList); // Debug log
        for (ClientHandler client : onlineUsers.values()) {
            client.sendMessage("USERS:" + userList);
        }
    }

    public void addOnlineUser(String username, ClientHandler client) {
        onlineUsers.put(username, client);
        broadcastUserList();
    }

    public void removeOnlineUser(String username) {
        onlineUsers.remove(username);
        broadcastUserList();
    }

    public void sendPrivateMessage(String sender, String receiver, String message) {
        // Lưu tin nhắn vào CSV trước
        saveMessage(sender, receiver, message);
        
        // Gửi tin nhắn cho người nhận nếu họ đang online
        ClientHandler receiverClient = onlineUsers.get(receiver);
        if (receiverClient != null) {
            receiverClient.sendMessage("MESSAGE:" + sender + ":" + message);
        }
    }

    public void sendPrivateFile(String sender, String receiver, String fileName, String filePath) {
        ClientHandler receiverClient = onlineUsers.get(receiver);
        if (receiverClient != null) {
            receiverClient.sendMessage("FILE:" + sender + ":" + fileName + ":" + filePath);
        }
    }

    public void sendGroupFile(String groupId, String sender, String fileName, String filePath) {
        // Gửi file cho tất cả thành viên trong group
        for (ClientHandler client : onlineUsers.values()) {
            client.sendMessage("GROUP_FILE:" + groupId + ":" + sender + ":" + fileName + ":" + filePath);
        }
    }

    private void saveMessage(String sender, String receiver, String message) {
        fileLock.lock();
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(MESSAGES_FILE, true));
            String[] record = {
                sender,
                receiver,
                message,
                String.valueOf(System.currentTimeMillis()),
                "" // deleted_by column
            };
            writer.writeNext(record);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fileLock.unlock();
        }
    }

    private void saveGroupMessage(String groupId, String sender, String message) {
        fileLock.lock();
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(GROUP_MESSAGES_FILE, true));
            // Thêm cột deleted_by để lưu danh sách người dùng đã xóa tin nhắn
            String[] record = {groupId, sender, message, String.valueOf(System.currentTimeMillis()), ""};
            writer.writeNext(record);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fileLock.unlock();
        }
    }

    public void loadChatHistory(String user1, String user2, ClientHandler client) {
        fileLock.lock();
        try {
            CSVReader reader = new CSVReader(new FileReader(MESSAGES_FILE));
            String[] record;
            while ((record = reader.readNext()) != null) {
                String sender = record[0];
                String receiver = record[1];
                String message = record[2];
                String deletedBy = record.length > 4 ? record[4] : "";
                
                // Kiểm tra nếu tin nhắn giữa user1 và user2
                if ((sender.equals(user1) && receiver.equals(user2)) || 
                    (sender.equals(user2) && receiver.equals(user1))) {
                    // Kiểm tra nếu tin nhắn chưa bị xóa bởi user1
                    if (!deletedBy.contains(user1)) {
                        // Gửi tin nhắn cho user1
                        client.sendMessage("HISTORY:" + sender + ":" + message);
                    }
                    
                    // Nếu user2 đang online và tin nhắn chưa bị xóa bởi user2
                    ClientHandler user2Client = onlineUsers.get(user2);
                    if (user2Client != null && !deletedBy.contains(user2)) {
                        user2Client.sendMessage("HISTORY:" + sender + ":" + message);
                    }
                }
            }
            reader.close();
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        } finally {
            fileLock.unlock();
        }
    }

    public void loadGroupHistory(String groupId, ClientHandler client) {
        fileLock.lock();
        try {
            CSVReader reader = new CSVReader(new FileReader(GROUP_MESSAGES_FILE));
            String[] record;
            while ((record = reader.readNext()) != null) {
                String recordGroupId = record[0];
                String sender = record[1];
                String message = record[2];
                String deletedBy = record.length > 4 ? record[4] : "";
                
                if (recordGroupId.equals(groupId) && !deletedBy.contains(client.getUsername())) {
                    client.sendMessage("GROUP_HISTORY:" + groupId + ":" + sender + ":" + message);
                }
            }
            reader.close();
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        } finally {
            fileLock.unlock();
        }
    }

    public void clearChatHistory(String user1, String user2) {
        fileLock.lock();
        try {
            // Đọc tất cả tin nhắn
            CSVReader reader = new CSVReader(new FileReader(MESSAGES_FILE));
            java.util.List<String[]> allMessages = new java.util.ArrayList<>();
            String[] record;
            while ((record = reader.readNext()) != null) {
                String sender = record[0];
                String receiver = record[1];
                String message = record[2];
                String timestamp = record[3];
                String deletedBy = record.length > 4 ? record[4] : "";

                // Nếu tin nhắn giữa user1 và user2
                if ((sender.equals(user1) && receiver.equals(user2)) || 
                    (sender.equals(user2) && receiver.equals(user1))) {
                    // Thêm user1 vào danh sách đã xóa
                    if (!deletedBy.contains(user1)) {
                        deletedBy = deletedBy.isEmpty() ? user1 : deletedBy + "," + user1;
                    }
                    String[] newRecord = {sender, receiver, message, timestamp, deletedBy};
                    allMessages.add(newRecord);
                } else {
                    allMessages.add(record);
                }
            }
            reader.close();

            // Xóa file cũ và tạo file mới
            File file = new File(MESSAGES_FILE);
            file.delete();
            file.createNewFile();

            // Ghi lại file với các tin nhắn đã cập nhật
            CSVWriter writer = new CSVWriter(new FileWriter(MESSAGES_FILE, false));
            for (String[] message : allMessages) {
                writer.writeNext(message);
            }
            writer.close();

            // Thông báo cho user2
            ClientHandler user2Client = onlineUsers.get(user2);
            if (user2Client != null) {
                user2Client.sendMessage("MESSAGE:" + user1 + ":Chat history has been cleared by " + user1);
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        } finally {
            fileLock.unlock();
        }
    }

    public void clearGroupHistory(String groupId, String username) {
        fileLock.lock();
        try {
            // Đọc tất cả tin nhắn
            CSVReader reader = new CSVReader(new FileReader(GROUP_MESSAGES_FILE));
            java.util.List<String[]> allMessages = new java.util.ArrayList<>();
            String[] record;
            while ((record = reader.readNext()) != null) {
                String recordGroupId = record[0];
                String sender = record[1];
                String message = record[2];
                String timestamp = record[3];
                String deletedBy = record.length > 4 ? record[4] : "";

                // Nếu tin nhắn thuộc group này
                if (recordGroupId.equals(groupId)) {
                    // Thêm username vào danh sách đã xóa
                    if (!deletedBy.contains(username)) {
                        deletedBy = deletedBy.isEmpty() ? username : deletedBy + "," + username;
                    }
                    String[] newRecord = {recordGroupId, sender, message, timestamp, deletedBy};
                    allMessages.add(newRecord);
                } else {
                    allMessages.add(record);
                }
            }
            reader.close();

            // Xóa file cũ và tạo file mới
            File file = new File(GROUP_MESSAGES_FILE);
            file.delete();
            file.createNewFile();

            // Ghi lại file với các tin nhắn đã cập nhật
            CSVWriter writer = new CSVWriter(new FileWriter(GROUP_MESSAGES_FILE, false));
            for (String[] message : allMessages) {
                writer.writeNext(message);
            }
            writer.close();

            // Thông báo cho các thành viên khác trong group
            Set<String> members = groupMembers.get(groupId);
            if (members != null) {
                for (String member : members) {
                    if (!member.equals(username)) {
                        ClientHandler memberClient = onlineUsers.get(member);
                        if (memberClient != null) {
                            memberClient.sendMessage("GROUP:" + groupId + ":" + username + ":Chat history has been cleared by " + username);
                        }
                    }
                }
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        } finally {
            fileLock.unlock();
        }
    }

    public void deleteMessage(String user1, String user2, String message) {
        fileLock.lock();
        try {
            // Đọc tất cả tin nhắn
            CSVReader reader = new CSVReader(new FileReader(MESSAGES_FILE));
            List<String[]> allMessages = new ArrayList<>();
            String[] record;
            while ((record = reader.readNext()) != null) {
                String sender = record[0];
                String receiver = record[1];
                String msg = record[2];
                String timestamp = record[3];
                String deletedBy = record.length > 4 ? record[4] : "";

                // Nếu là tin nhắn cần xóa
                if (((sender.equals(user1) && receiver.equals(user2)) || 
                     (sender.equals(user2) && receiver.equals(user1))) && 
                    msg.equals(message)) {
                    // Thêm user1 vào danh sách đã xóa
                    if (!deletedBy.contains(user1)) {
                        deletedBy = deletedBy.isEmpty() ? user1 : deletedBy + "," + user1;
                    }
                    String[] newRecord = {sender, receiver, msg, timestamp, deletedBy};
                    allMessages.add(newRecord);
                } else {
                    allMessages.add(record);
                }
            }
            reader.close();

            // Ghi lại file với tin nhắn đã cập nhật
            CSVWriter writer = new CSVWriter(new FileWriter(MESSAGES_FILE, false));
            for (String[] msg : allMessages) {
                writer.writeNext(msg);
            }
            writer.close();
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        } finally {
            fileLock.unlock();
        }
    }

    public void deleteGroupMessage(String groupId, String username, String message) {
        fileLock.lock();
        try {
            // Đọc tất cả tin nhắn
            CSVReader reader = new CSVReader(new FileReader(GROUP_MESSAGES_FILE));
            List<String[]> allMessages = new ArrayList<>();
            String[] record;
            while ((record = reader.readNext()) != null) {
                String recordGroupId = record[0];
                String sender = record[1];
                String msg = record[2];
                String timestamp = record[3];
                String deletedBy = record.length > 4 ? record[4] : "";

                // Nếu là tin nhắn cần xóa
                if (recordGroupId.equals(groupId) && msg.equals(message)) {
                    // Thêm username vào danh sách đã xóa
                    if (!deletedBy.contains(username)) {
                        deletedBy = deletedBy.isEmpty() ? username : deletedBy + "," + username;
                    }
                    String[] newRecord = {recordGroupId, sender, msg, timestamp, deletedBy};
                    allMessages.add(newRecord);
                } else {
                    allMessages.add(record);
                }
            }
            reader.close();

            // Ghi lại file với tin nhắn đã cập nhật
            CSVWriter writer = new CSVWriter(new FileWriter(GROUP_MESSAGES_FILE, false));
            for (String[] msg : allMessages) {
                writer.writeNext(msg);
            }
            writer.close();
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        } finally {
            fileLock.unlock();
        }
    }

    public void handleGetGroupMembers(String groupId, ClientHandler client) {
        fileLock.lock();
        try {
            Set<String> members = groupMembers.get(groupId);
            if (members != null) {
                String memberList = String.join(",", members);
                client.sendMessage("GROUP_MEMBERS:" + groupId + ":" + memberList);
            }
        } finally {
            fileLock.unlock();
        }
    }

    public void sendGroupListToUser(ClientHandler client) {
        String username = client.getUsername();
        StringBuilder userGroups = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : groupMembers.entrySet()) {
            if (entry.getValue().contains(username)) {
                if (userGroups.length() > 0) {
                    userGroups.append(";");
                }
                userGroups.append(entry.getKey()).append(":").append(String.join(",", entry.getValue()));
            }
        }
        client.sendMessage("GROUP_LIST:" + userGroups.toString());
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start();
    }
} 