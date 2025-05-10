package com.chatapp.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
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
import javax.swing.text.Utilities;

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
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Create main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create left panel
        JPanel leftPanel = createLeftPanel();

        // Create chat tabs
        chatTabs = new JTabbedPane();
        chatTabs.setTabPlacement(JTabbedPane.TOP);

        // Add components to main panel
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(chatTabs, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, 0));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

        // Tabs for Users and Groups
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Users Panel
        JPanel usersPanel = new JPanel(new BorderLayout());
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
        JPanel groupsPanel = new JPanel(new BorderLayout());
        groupList = new JList<>();
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedGroup = groupList.getSelectedValue();
                if (selectedGroup != null) {
                    openGroupChat(selectedGroup);
                }
            }
        });
        groupsPanel.add(new JScrollPane(groupList), BorderLayout.CENTER);
        
        // Group Management Buttons
        JPanel groupButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton createGroupButton = new JButton("Create Group");
        createGroupButton.addActionListener(e -> showCreateGroupDialog());
        groupButtonsPanel.add(createGroupButton);
        groupsPanel.add(groupButtonsPanel, BorderLayout.SOUTH);
        
        tabbedPane.addTab("Users", usersPanel);
        tabbedPane.addTab("Groups", groupsPanel);
        
        leftPanel.add(tabbedPane, BorderLayout.CENTER);
        return leftPanel;
    }

    private void showCreateGroupDialog() {
        String groupId = JOptionPane.showInputDialog(this, "Enter group name:", "Create Group", JOptionPane.PLAIN_MESSAGE);
        if (groupId != null && !groupId.trim().isEmpty()) {
            chatClient.sendMessage("CREATE_GROUP:" + groupId);
        }
    }

    private void openGroupChat(String groupId) {
        if (!chatPanels.containsKey(groupId)) {
            ChatPanel groupPanel = new ChatPanel(chatClient, groupId, true);
            chatPanels.put(groupId, groupPanel);
            chatTabs.addTab(groupId, groupPanel);
        }
        chatTabs.setSelectedComponent(chatPanels.get(groupId));
    }

    private void openChat(String username) {
        if (!chatPanels.containsKey(username)) {
            ChatPanel chatPanel = new ChatPanel(chatClient, username);
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
            chatPanels.get(groupId).addMessage(sender, message);
        });
    }

    public void receiveFile(String sender, String fileName, String filePath) {
        SwingUtilities.invokeLater(() -> {
            if (!chatPanels.containsKey(sender)) {
                openChat(sender);
            }
            chatPanels.get(sender).addFileMessage(sender, fileName, filePath);
        });
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
        if (chatPanels.containsKey(groupId)) {
            ChatPanel panel = chatPanels.get(groupId);
            panel.addMessage(sender, message);
        }
    }
}

class ChatPanel extends JPanel {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton fileButton;
    private JButton clearHistoryButton;
    private ChatClient chatClient;
    private String target;
    private boolean isGroup;
    private Map<String, Integer> messageLines;
    private JPopupMenu contextMenu;
    private String selectedMessage;

    public ChatPanel(ChatClient chatClient, String target) {
        this(chatClient, target, false);
    }

    public ChatPanel(ChatClient chatClient, String target, boolean isGroup) {
        this.chatClient = chatClient;
        this.target = target;
        this.isGroup = isGroup;
        this.messageLines = new HashMap<>();
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        add(chatScroll, BorderLayout.CENTER);

        // Message input panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Send");
        fileButton = new JButton("Send File");
        clearHistoryButton = new JButton("Clear History");

        // Tạo menu ngữ cảnh
        contextMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete Message");
        contextMenu.add(deleteItem);

        // Thêm mouse listener cho chat area
        chatArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    try {
                        int line = chatArea.getLineOfOffset(chatArea.viewToModel2D(e.getPoint()));
                        String message = getMessageAtLine(line);
                        if (message != null) {
                            selectedMessage = message;
                            contextMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    try {
                        int line = chatArea.getLineOfOffset(chatArea.viewToModel2D(e.getPoint()));
                        String message = getMessageAtLine(line);
                        if (message != null) {
                            selectedMessage = message;
                            contextMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        // Thêm action listener cho menu item
        deleteItem.addActionListener(e -> {
            if (selectedMessage != null) {
                deleteSelectedMessage();
            }
        });

        messageField.addActionListener(e -> sendMessage());
        sendButton.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> sendFile());
        clearHistoryButton.addActionListener(e -> clearHistory());

        // Load chat history
        if (isGroup) {
            chatClient.sendMessage("GROUP_HISTORY:" + target);
        } else {
            chatClient.sendMessage("HISTORY:" + target);
        }

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(fileButton, BorderLayout.WEST);
        buttonPanel.add(sendButton, BorderLayout.EAST);
        buttonPanel.add(clearHistoryButton, BorderLayout.CENTER);

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        add(inputPanel, BorderLayout.SOUTH);
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

    public void addMessage(String sender, String message) {
        String formattedMessage = sender + ": " + message;
        chatArea.append(formattedMessage + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
        messageLines.put(message, chatArea.getLineCount() - 1);
    }

    public void addFileMessage(String sender, String fileName, String filePath) {
        chatArea.append(sender + " sent a file: " + fileName + "\n");
        chatArea.append("[Click to download: " + filePath + "]\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
        
        // Thêm hyperlink cho file
        chatArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int offset = chatArea.viewToModel2D(e.getPoint());
                try {
                    int rowStart = Utilities.getRowStart(chatArea, offset);
                    int rowEnd = Utilities.getRowEnd(chatArea, offset);
                    String line = chatArea.getText(rowStart, rowEnd - rowStart);
                    
                    if (line.contains("[Click to download:")) {
                        String path = line.substring(line.indexOf(":") + 1, line.indexOf("]")).trim();
                        downloadFile(path);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void downloadFile(String filePath) {
        try {
            File sourceFile = new File(filePath);
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(sourceFile.getName()));
            
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File destFile = fileChooser.getSelectedFile();
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(this, "File downloaded successfully!");
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error downloading file: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void clearMessages() {
        chatArea.setText("");
        messageLines.clear();
    }
} 