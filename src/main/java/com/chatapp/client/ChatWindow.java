package com.chatapp.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

public class ChatWindow extends JFrame {
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JTabbedPane chatTabs;
    private Map<String, ChatPanel> chatPanels;
    private ChatClient chatClient;
    private String currentUser;
    private JList<String> groupList;

    public ChatWindow(ChatClient chatClient, String username) {
        this.chatClient = chatClient;
        this.currentUser = username;
        this.chatPanels = new HashMap<>();
        this.userListModel = new DefaultListModel<>();
        setupUI();
    }

    private void setupUI() {
        setTitle("Chat Application - " + currentUser);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        // Set look and feel colors
        Color backgroundColor = new Color(240, 240, 240);
        Color panelColor = new Color(255, 255, 255);
        Color accentColor = new Color(0, 120, 212);
        Color textColor = new Color(51, 51, 51);
        Font mainFont = new Font("Segoe UI", Font.PLAIN, 14);
        Font headerFont = new Font("Segoe UI", Font.BOLD, 16);

        // Create main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(backgroundColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create left panel with improved styling
        JPanel leftPanel = createLeftPanel();
        leftPanel.setBackground(panelColor);
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Create chat tabs with custom styling
        chatTabs = new JTabbedPane();
        chatTabs.setTabPlacement(JTabbedPane.TOP);
        chatTabs.setFont(mainFont);
        chatTabs.setBackground(panelColor);
        chatTabs.setForeground(textColor);

        // Add components to main panel
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(chatTabs, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setPreferredSize(new Dimension(250, 0));

        // Tabs for Users and Groups with custom styling
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        // Users Panel
        JPanel usersPanel = new JPanel(new BorderLayout(5, 5));
        usersPanel.setBackground(new Color(255, 255, 255));
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setBackground(new Color(250, 250, 250));
        userList.setForeground(new Color(51, 51, 51));
        userList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUser = userList.getSelectedValue();
                if (selectedUser != null) {
                    openChat(selectedUser);
                }
            }
        });
        usersPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        
        // Groups Panel
        JPanel groupsPanel = new JPanel(new BorderLayout(5, 5));
        groupsPanel.setBackground(new Color(255, 255, 255));
        groupList = new JList<>();
        groupList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.setBackground(new Color(250, 250, 250));
        groupList.setForeground(new Color(51, 51, 51));
        groupList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedGroup = groupList.getSelectedValue();
                if (selectedGroup != null) {
                    openGroupChat(selectedGroup);
                }
            }
        });
        groupsPanel.add(new JScrollPane(groupList), BorderLayout.CENTER);
        
        // Group Management Buttons with improved styling
        JPanel groupButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        groupButtonsPanel.setBackground(new Color(255, 255, 255));
        JButton createGroupButton = createStyledButton("Create Group");
        JButton addToGroupButton = createStyledButton("Add User");
        createGroupButton.addActionListener(e -> showCreateGroupDialog());
        addToGroupButton.addActionListener(e -> showAddToGroupDialog());
        groupButtonsPanel.add(createGroupButton);
        groupButtonsPanel.add(addToGroupButton);
        groupsPanel.add(groupButtonsPanel, BorderLayout.SOUTH);
        
        tabbedPane.addTab("Users", usersPanel);
        tabbedPane.addTab("Groups", groupsPanel);
        
        leftPanel.add(tabbedPane, BorderLayout.CENTER);
        return leftPanel;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setBackground(new Color(0, 120, 212));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(100, 30));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0, 100, 180));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0, 120, 212));
            }
        });
        return button;
    }

    private void setupChatPanel(ChatPanel panel) {
        panel.setBackground(new Color(255, 255, 255));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void showCreateGroupDialog() {
        String groupId = JOptionPane.showInputDialog(this, "Enter group name:", "Create Group", JOptionPane.PLAIN_MESSAGE);
        if (groupId != null && !groupId.trim().isEmpty()) {
            chatClient.sendMessage("CREATE_GROUP:" + groupId);
        }
    }

    private void showAddToGroupDialog() {
        String selectedGroup = groupList.getSelectedValue();
        if (selectedGroup == null) {
            JOptionPane.showMessageDialog(this, 
                "Please select a group first", 
                "No Group Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String username = JOptionPane.showInputDialog(this, 
            "Enter username to add to group:", 
            "Add User to Group", 
            JOptionPane.PLAIN_MESSAGE);
            
        if (username != null && !username.trim().isEmpty()) {
            chatClient.sendMessage("ADD_TO_GROUP:" + selectedGroup + ":" + username);
        }
    }

    private void openGroupChat(String groupId) {
        if (!chatPanels.containsKey(groupId)) {
            ChatPanel groupPanel = new ChatPanel(chatClient, groupId, true);
            setupChatPanel(groupPanel);
            chatPanels.put(groupId, groupPanel);
            chatTabs.addTab(groupId, groupPanel);
            // Request group chat history
            chatClient.sendMessage("GROUP_HISTORY:" + groupId);
        }
        chatTabs.setSelectedComponent(chatPanels.get(groupId));
    }

    private void openChat(String username) {
        if (!chatPanels.containsKey(username)) {
            ChatPanel chatPanel = new ChatPanel(chatClient, username);
            setupChatPanel(chatPanel);
            chatPanels.put(username, chatPanel);
            chatTabs.addTab(username, chatPanel);
            // Request chat history
            chatClient.sendMessage("HISTORY:" + username);
            // Clear any existing messages in the panel
            chatPanel.clearMessages();
        }
        chatTabs.setSelectedComponent(chatPanels.get(username));
    }

    public void updateUserList(String[] users) {
        userListModel.clear();
        for (String user : users) {
            if (!user.equals(currentUser)) {
                userListModel.addElement(user);
            }
        }
    }

    public void receiveMessage(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            if (!chatPanels.containsKey(sender)) {
                openChat(sender);
            }
            chatPanels.get(sender).addMessage(sender, message);
        });
    }

    public void receiveGroupMessage(String groupId, String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            if (!chatPanels.containsKey(groupId)) {
                ChatPanel groupPanel = new ChatPanel(chatClient, groupId, true);
                chatPanels.put(groupId, groupPanel);
                chatTabs.addTab(groupId, groupPanel);
            }
            ChatPanel panel = chatPanels.get(groupId);
            // Kiểm tra xem tin nhắn đã tồn tại chưa
            if (!panel.hasMessage(message)) {
                panel.addMessage(sender, message);
            }
        });
    }

    public void receiveFile(String sender, String fileName, String filePath) {
        // Không mở chat panel tự động khi nhận file
        if (!sender.equals(currentUser)) {
            // Kiểm tra xem đã có chat tab chưa
            ChatPanel panel = chatPanels.get(sender);
            if (panel == null) {
                openChat(sender);
                panel = chatPanels.get(sender);
            }
            panel.addFileMessage(sender, fileName, filePath);
            // Highlight tab để thông báo có tin nhắn mới
            int index = getTabIndex(sender);
            if (index >= 0) {
                chatTabs.setForegroundAt(index, Color.RED);
            }
        }
    }

    public void receiveGroupFile(String groupId, String sender, String fileName, String filePath) {
        SwingUtilities.invokeLater(() -> {
            if (!chatPanels.containsKey(groupId)) {
                ChatPanel groupPanel = new ChatPanel(chatClient, groupId, true);
                chatPanels.put(groupId, groupPanel);
                chatTabs.addTab(groupId, groupPanel);
            }
            chatPanels.get(groupId).addFileMessage(sender, fileName, filePath);
        });
    }

    public void updateGroupList(DefaultListModel<String> groupListModel) {
        groupList.setModel(groupListModel);
    }

    public void addGroupMessage(String groupId, String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            if (!chatPanels.containsKey(groupId)) {
                ChatPanel groupPanel = new ChatPanel(chatClient, groupId, true);
                chatPanels.put(groupId, groupPanel);
                chatTabs.addTab(groupId, groupPanel);
            }
            chatPanels.get(groupId).addMessage(sender, message);
        });
    }

    public void addGroup(String groupId) {
        DefaultListModel<String> groupListModel = (DefaultListModel<String>) groupList.getModel();
        if (!groupListModel.contains(groupId)) {
            groupListModel.addElement(groupId);
        }
    }

    public void addGroupMember(String groupId, String username) {
        if (chatPanels.containsKey(groupId)) {
            ChatPanel panel = (ChatPanel) chatPanels.get(groupId);
            panel.addMember(username);
        }
    }

    public void updateGroupMembers(String groupId, String[] members) {
        if (chatPanels.containsKey(groupId)) {
            ChatPanel panel = (ChatPanel) chatPanels.get(groupId);
            panel.updateMemberList(members);
        }
    }

    private int getTabIndex(String title) {
        for (int i = 0; i < chatTabs.getTabCount(); i++) {
            if (chatTabs.getTitleAt(i).equals(title)) {
                return i;
            }
        }
        return -1;
    }
}

class ChatPanel extends JPanel {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton fileButton;
    private JButton clearHistoryButton;
    private JButton inviteButton;
    private JButton showMembersButton;
    private ChatClient chatClient;
    private String target;
    private boolean isGroup;
    private Map<String, Integer> messageLines;
    private JPopupMenu contextMenu;
    private String selectedMessage;
    private JList<String> memberList;
    private DefaultListModel<String> memberListModel;
    private Set<String> messageSet = new HashSet<>();

    public ChatPanel(ChatClient chatClient, String target) {
        this(chatClient, target, false);
    }

    public ChatPanel(ChatClient chatClient, String target, boolean isGroup) {
        this.chatClient = chatClient;
        this.target = target;
        this.isGroup = isGroup;
        this.messageLines = new HashMap<>();
        this.memberListModel = new DefaultListModel<>();
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(new Color(255, 255, 255));

        // Chat area with improved styling
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setBackground(new Color(250, 250, 250));
        chatArea.setForeground(new Color(51, 51, 51));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setPreferredSize(new Dimension(600, 400));
        chatScroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        add(chatScroll, BorderLayout.CENTER);

        // Message input panel with improved styling
        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.setBackground(new Color(255, 255, 255));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Message field with improved styling
        JPanel messagePanel = new JPanel(new BorderLayout(5, 0));
        messageField = new JTextField();
        messageField.setPreferredSize(new Dimension(400, 30));
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        messagePanel.add(messageField, BorderLayout.CENTER);

        // Button panel with improved styling
        JPanel buttonPanel = new JPanel(new BorderLayout(10, 0));
        buttonPanel.setBackground(new Color(255, 255, 255));
        
        // Left buttons panel
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftButtons.setBackground(new Color(255, 255, 255));
        fileButton = createStyledButton("Send File");
        leftButtons.add(fileButton);
        
        if (isGroup) {
            inviteButton = createStyledButton("Invite");
            showMembersButton = createStyledButton("Members");
            leftButtons.add(inviteButton);
            leftButtons.add(showMembersButton);
        }
        
        // Right buttons panel
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightButtons.setBackground(new Color(255, 255, 255));
        clearHistoryButton = createStyledButton("Clear");
        sendButton = createStyledButton("Send");
        rightButtons.add(clearHistoryButton);
        rightButtons.add(sendButton);

        buttonPanel.add(leftButtons, BorderLayout.WEST);
        buttonPanel.add(rightButtons, BorderLayout.EAST);

        // Add components to input panel
        inputPanel.add(messagePanel, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(inputPanel, BorderLayout.SOUTH);

        // Add action listeners
        messageField.addActionListener(e -> sendMessage());
        sendButton.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> sendFile());
        clearHistoryButton.addActionListener(e -> clearHistory());
        if (isGroup) {
            inviteButton.addActionListener(e -> showInviteDialog());
            showMembersButton.addActionListener(e -> showMembersDialog());
        }

        // Load chat history
        if (isGroup) {
            chatClient.sendMessage("GROUP_HISTORY:" + target);
            chatClient.sendMessage("GET_GROUP_MEMBERS:" + target);
        } else {
            chatClient.sendMessage("HISTORY:" + target);
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setBackground(new Color(0, 120, 212));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(100, 30));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0, 100, 180));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0, 120, 212));
            }
        });
        return button;
    }

    private String getMessageAtLine(int line) {
        try {
            int start = chatArea.getLineStartOffset(line);
            int end = chatArea.getLineEndOffset(line);
            String lineText = chatArea.getText(start, end - start).trim();
            if (lineText.startsWith("You: ") || lineText.startsWith(target + ": ")) {
                return lineText.substring(lineText.indexOf(": ") + 2);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void deleteSelectedMessage() {
        if (selectedMessage != null) {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this message?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                if (isGroup) {
                    chatClient.sendMessage("DELETE_GROUP_MESSAGE:" + target + ":" + selectedMessage);
                } else {
                    chatClient.sendMessage("DELETE_MESSAGE:" + target + ":" + selectedMessage);
                }
                // Xóa tin nhắn khỏi giao diện
                removeMessage(selectedMessage);
            }
        }
    }

    private void removeMessage(String message) {
        Integer line = messageLines.get(message);
        if (line != null) {
            try {
                int start = chatArea.getLineStartOffset(line);
                int end = chatArea.getLineEndOffset(line);
                chatArea.replaceRange("", start, end);
                messageLines.remove(message);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            if (isGroup) {
                chatClient.sendMessage("GROUP:" + target + ":" + message);
            } else {
                chatClient.sendMessage("MESSAGE:" + target + ":" + message);
            }
            addMessage("You", message);
            messageField.setText("");
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            chatClient.sendFile(target, file, isGroup);
            addFileMessage("You", file.getName(), file.getAbsolutePath());
        }
    }

    private void clearHistory() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to clear chat history? This action cannot be undone.",
            "Confirm Clear History",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            if (isGroup) {
                chatClient.sendMessage("CLEAR_GROUP_HISTORY:" + target);
            } else {
                chatClient.sendMessage("CLEAR_HISTORY:" + target);
            }
            chatArea.setText("");
            messageLines.clear();
            // Add notification message
            chatArea.append("Chat history has been cleared.\n");
        }
    }

    private void showInviteDialog() {
        String username = JOptionPane.showInputDialog(this, 
            "Enter username to invite to group:", 
            "Invite User to Group", 
            JOptionPane.PLAIN_MESSAGE);
            
        if (username != null && !username.trim().isEmpty()) {
            chatClient.sendMessage("ADD_TO_GROUP:" + target + ":" + username);
        }
    }

    private void showMembersDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Group Members - " + target, true);
        dialog.setLayout(new BorderLayout());
        
        memberList = new JList<>(memberListModel);
        JScrollPane scrollPane = new JScrollPane(memberList);
        dialog.add(scrollPane, BorderLayout.CENTER);
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        dialog.add(closeButton, BorderLayout.SOUTH);
        
        // Request updated member list from server
        chatClient.sendMessage("GET_GROUP_MEMBERS:" + target);
        
        dialog.setSize(200, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public void updateMemberList(String[] members) {
        memberListModel.clear();
        for (String member : members) {
            memberListModel.addElement(member);
        }
    }

    public void addMessage(String sender, String message) {
        String formattedMessage = sender + ": " + message;
        if (!messageSet.contains(message)) {
            messageSet.add(message);
            chatArea.append(formattedMessage + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
            messageLines.put(message, chatArea.getLineCount() - 1);
        }
    }

    public void addFileMessage(String sender, String fileName, String filePath) {
        String formattedMessage = sender + " sent a file: 📎 " + fileName + "\n";
        chatArea.append(formattedMessage);
        chatArea.append("📥 [Click here to download the file]\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
        
        final int lastLineNum = chatArea.getLineCount() - 1;
        final String finalFilePath = filePath;
        
        // Xóa tất cả mouse listener cũ để tránh conflict
        for (MouseListener ml : chatArea.getMouseListeners()) {
            if (ml instanceof MouseAdapter) {
                chatArea.removeMouseListener(ml);
            }
        }
        
        // Thêm hyperlink cho file
        chatArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    int offset = chatArea.viewToModel2D(e.getPoint());
                    int clickedLine = chatArea.getLineOfOffset(offset);
                    
                    if (clickedLine == lastLineNum) {
                        downloadAndOpenFile(finalFilePath);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ChatPanel.this, 
                        "Error handling file: " + ex.getMessage(),
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void downloadAndOpenFile(String filePath) {
        try {
            // Kiểm tra xem file có tồn tại trên server không
            File sourceFile = new File(filePath);
            if (!sourceFile.exists()) {
                JOptionPane.showMessageDialog(this, 
                    "File not found on server: " + filePath,
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Trích xuất tên file từ đường dẫn đầy đủ
            String fileName = sourceFile.getName();
            
            // Cho phép người dùng chọn nơi lưu file
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(fileName));
            fileChooser.setDialogTitle("Save File");
            
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File destFile = fileChooser.getSelectedFile();
                
                // Đảm bảo file đích có đuôi file đúng
                if (!destFile.getName().contains(".")) {
                    String extension = "";
                    int i = sourceFile.getName().lastIndexOf('.');
                    if (i > 0) {
                        extension = sourceFile.getName().substring(i);
                    }
                    destFile = new File(destFile.getAbsolutePath() + extension);
                }

                // Copy file từ server về máy local
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                // Mở file sau khi tải xuống
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.OPEN)) {
                        try {
                            desktop.open(destFile);
                            JOptionPane.showMessageDialog(this, 
                                "File downloaded and opened successfully!\nLocation: " + destFile.getAbsolutePath(),
                                "Success", 
                                JOptionPane.INFORMATION_MESSAGE);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(this, 
                                "File downloaded but could not be opened automatically.\nLocation: " + destFile.getAbsolutePath(),
                                "Warning", 
                                JOptionPane.WARNING_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "File downloaded successfully, but cannot be opened automatically.\nLocation: " + destFile.getAbsolutePath(),
                            "Warning", 
                            JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "File downloaded successfully, but cannot be opened automatically.\nLocation: " + destFile.getAbsolutePath(),
                        "Warning", 
                        JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error downloading file: " + e.getMessage(),
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public void clearMessages() {
        chatArea.setText("");
        messageLines.clear();
    }

    public void addMember(String username) {
        if (!memberListModel.contains(username)) {
            memberListModel.addElement(username);
        }
    }

    public boolean hasMessage(String message) {
        return messageSet.contains(message);
    }
} 