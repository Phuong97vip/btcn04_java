package com.chatapp.client;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField emailField;
    private JButton loginButton;
    private JButton registerButton;
    private JButton switchModeButton;
    private boolean isLoginMode = true;
    private ChatClient chatClient;

    public LoginWindow(ChatClient chatClient) {
        this.chatClient = chatClient;
        setupUI();
    }

    private void setupUI() {
        setTitle("Chat Application - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        mainPanel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        mainPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        mainPanel.add(passwordField, gbc);

        // Email (initially hidden)
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        emailField = new JTextField(20);
        emailField.setVisible(false);
        mainPanel.add(new JLabel("Email:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        mainPanel.add(emailField, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");
        switchModeButton = new JButton("Switch to Register");

        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> handleRegister());
        switchModeButton.addActionListener(e -> switchMode());

        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        buttonPanel.add(switchModeButton);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel);
        updateMode();
    }

    private void switchMode() {
        isLoginMode = !isLoginMode;
        updateMode();
    }

    private void updateMode() {
        if (isLoginMode) {
            setTitle("Chat Application - Login");
            emailField.setVisible(false);
            loginButton.setVisible(true);
            registerButton.setVisible(false);
            switchModeButton.setText("Switch to Register");
        } else {
            setTitle("Chat Application - Register");
            emailField.setVisible(true);
            loginButton.setVisible(false);
            registerButton.setVisible(true);
            switchModeButton.setText("Switch to Login");
        }
        pack();
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields");
            return;
        }

        chatClient.sendMessage("LOGIN:" + username + ":" + password);
    }

    private void handleRegister() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String email = emailField.getText();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields");
            return;
        }

        chatClient.sendMessage("REGISTER:" + username + ":" + password + ":" + email);
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public String getUsername() {
        return usernameField.getText();
    }
} 