# Java Chat Application

A real-time chat application built with Java Swing, featuring client-server architecture and CSV-based data storage.

## Features

- User registration and authentication
- Real-time private messaging
- Group chat functionality
- File sharing
- Chat history
- Online user list
- Modern Swing GUI

## Requirements

- Java 11 or higher
- Maven 3.6 or higher

## Dependencies

- OpenCSV 5.7.1
- Apache Commons IO 2.11.0

## Building the Application

1. Clone the repository
2. Navigate to the project directory
3. Build the project using Maven:

```bash
mvn clean package
```

This will create two JAR files in the `target` directory:
- `chat-application-1.0-SNAPSHOT-jar-with-dependencies.jar` (Server)
- `chat-application-1.0-SNAPSHOT.jar` (Client)

## Running the Application

1. Start the server first:

```bash
java -jar target/chat-application-1.0-SNAPSHOT-jar-with-dependencies.jar
```

2. Start one or more clients:

```bash
java -jar target/chat-application-1.0-SNAPSHOT.jar
```

## Usage

1. Register a new account or login with existing credentials
2. View online users in the left panel
3. Click on a user to start a private chat
4. Use the "Create Group" button to create a group chat
5. Send messages and files using the chat interface

## File Structure

- `users.csv`: Stores user credentials
- `messages.csv`: Stores private messages
- `group_messages.csv`: Stores group chat messages
- `groups.csv`: Stores group information
- `server_files/`: Directory for shared files

## Security Notes

- Passwords are stored in plain text (for demonstration purposes)
- In a production environment, implement proper password hashing
- Add SSL/TLS for secure communication
- Implement proper input validation and sanitization

## License

This project is licensed under the MIT License - see the LICENSE file for details. 