package ie.dcu.secureYAC;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class UI extends Application {
    private VBox chatBox;
    private VBox contactsList;
    private HashMap<String, List<String[]>> messageHistory;
    private HashMap<String, HBox> contactBoxes;
    private HashMap<String, Label> lastMessageLabels;
    private String currentChat;
    private ImageView topBarProfileImage;
    private Label topBarUsername;
    private boolean contactsVisible = true;
    private SplitPane splitPane;
    private ScrollPane contactsScroll;
    private ServerThread serverThread;
    private String username;
    private int port;

    // Ports to try, starting from 8000
    private static final AtomicInteger NEXT_PORT = new AtomicInteger(8000);
    private static final int MAX_PORT_ATTEMPTS = 100;

    @Override
    public void start(Stage primaryStage) {
        showLoginDialog(primaryStage);
    }

    private void showLoginDialog(Stage primaryStage) {
        // Create dialog for username only
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("SecureYAC Login");
        dialog.setHeaderText("Enter your username");

        // Set the button types
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the username field
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default
        Platform.runLater(() -> usernameField.requestFocus());

        // Convert the result when the login button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return usernameField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(username -> {
            if (!username.isEmpty()) {
                this.username = username;

                // Find an available port automatically
                if (findAvailablePort()) {
                    try {
                        // Start the server thread with the automatically assigned port
                        serverThread = new ServerThread(String.valueOf(port));
                        serverThread.start();

                        // Now show the main UI
                        initMainUI(primaryStage);

                        // Show the peer connection dialog
                        showPeerConnectionDialog();
                    } catch (IOException e) {
                        showErrorAlert("Error", "Could not start server on port " + port, e.getMessage());
                    }
                } else {
                    showErrorAlert("Error", "Could not find an available port",
                            "Tried " + MAX_PORT_ATTEMPTS + " ports starting from " + NEXT_PORT.get() +
                                    " but none were available.");
                }
            }
        });
    }

    private boolean findAvailablePort() {
        int attempts = 0;
        while (attempts < MAX_PORT_ATTEMPTS) {
            int portToTry = NEXT_PORT.getAndIncrement();
            try (ServerSocket socket = new ServerSocket(portToTry)) {
                // Port is available, use it
                this.port = portToTry;
                return true;
            } catch (IOException e) {
                // Port is in use, try the next one
                attempts++;
            }
        }
        return false;
    }

    private void showPeerConnectionDialog() {
        // Create a dialog that explains the automatic port
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Connect to Peers");
        dialog.setHeaderText("Your server is running on port " + port);

        // Automatically populated information about the current user
        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(10));

        Label connectionInfo = new Label("Share these details with others to connect:");
        connectionInfo.setStyle("-fx-font-weight: bold;");

        TextField hostInfo = new TextField("localhost:" + port);
        hostInfo.setEditable(false);

        Button copyButton = new Button("Copy to Clipboard");
        copyButton.setOnAction(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(hostInfo.getText());
            clipboard.setContent(content);
        });

        infoBox.getChildren().addAll(connectionInfo, hostInfo, copyButton);

        // Section for connecting to others
        Label connectToOthers = new Label("Enter peers to connect to (hostname:port):");
        connectToOthers.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");

        TextField peersField = new TextField();
        peersField.setPromptText("hostname:port (separate multiple with spaces)");

        infoBox.getChildren().addAll(connectToOthers, peersField);

        dialog.getDialogPane().setContent(infoBox);

        // Set the button types
        ButtonType connectButtonType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        ButtonType skipButtonType = new ButtonType("Skip", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(connectButtonType, skipButtonType);

        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                return peersField.getText();
            }
            return "s"; // Skip
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(peers -> {
            if (!peers.equals("s")) {
                connectToPeers(peers);
            }
        });
    }

    private void connectToPeers(String peersInput) {
        if (!peersInput.isEmpty()) {
            String[] inputValues = peersInput.split(" ");
            for (String peerAddress : inputValues) {
                String[] address = peerAddress.split(":");
                if (address.length == 2) {
                    Socket socket = null;
                    try {
                        socket = new Socket(address[0], Integer.parseInt(address[1]));
                        PeerThread peerThread = new PeerThread(socket);
                        peerThread.setMessageHandler(this::handleIncomingMessage);
                        peerThread.start();

                        // Add the peer as a contact if not already added
                        Platform.runLater(() -> {
                            if (!messageHistory.containsKey(peerAddress)) {
                                addContact(peerAddress, "Connected",
                                        "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png");
                            }
                        });
                    } catch (Exception e) {
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException ioEx) {
                                // Ignore
                            }
                        }
                        showErrorAlert("Connection Error", "Failed to connect to " + peerAddress, e.getMessage());
                    }
                }
            }
        }
    }

    private void handleIncomingMessage(String message) {
        Platform.runLater(() -> {
            try {
                JSONObject jsonObject = new JSONObject(message);
                String sender = jsonObject.getString("username");
                String msgContent = jsonObject.getString("message");

                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy    HH:mm:ss");
                String timestamp = now.format(formatter);

                // If the sender is not in our contacts, add them
                if (!messageHistory.containsKey(sender)) {
                    addContact(sender, msgContent,
                            "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png");
                } else {
                    // Update the last message
                    messageHistory.get(sender).add(new String[]{sender, msgContent, timestamp});
                    updateLastMessage(sender, msgContent);

                    // If this is the current chat, update the chat box
                    if (currentChat != null && currentChat.equals(sender)) {
                        updateChatBox(sender);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing incoming message: " + e.getMessage());
            }
        });
    }

    private void initMainUI(Stage primaryStage) {
        // Main layout using BorderPane
        BorderPane root = new BorderPane();
        messageHistory = new HashMap<>();
        contactBoxes = new HashMap<>();
        lastMessageLabels = new HashMap<>();

        // Add contact button
        Button addContactButton = new Button("+");
        addContactButton.setStyle("-fx-font-size: 18px; -fx-background-color: transparent;");
        addContactButton.setMaxWidth(Double.MAX_VALUE);
        addContactButton.setOnAction(e -> showAddContact());

        // Left sidebar with contacts list
        contactsList = new VBox(10);
        contactsList.setPadding(new Insets(10));
        contactsList.setStyle("-fx-background-color: #e0e0e0;");

        HBox buttonRow = new HBox(10);
        buttonRow.getChildren().addAll(addContactButton);
        contactsList.getChildren().add(buttonRow);

        contactsScroll = new ScrollPane(contactsList);
        contactsScroll.setFitToWidth(true);
        contactsScroll.setFitToHeight(true);

        // Chat area
        BorderPane chatArea = new BorderPane();

        // Top bar with the person we are chatting with
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button menuButton = new Button("☰");
        menuButton.setStyle("-fx-font-size: 18px; -fx-background-color: transparent;");
        menuButton.setOnAction(e -> toggleContacts());

        topBarProfileImage = new ImageView(new Image("https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png"));
        topBarProfileImage.setFitWidth(50);
        topBarProfileImage.setFitHeight(50);
        topBarUsername = new Label("Select a Contact");
        topBarUsername.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // User status label (showing current username and port)
        Label userStatusLabel = new Label("Logged in as: " + username + " (Port: " + port + ")");
        userStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: grey;");

        VBox userInfoBox = new VBox(5);
        userInfoBox.getChildren().addAll(topBarUsername, userStatusLabel);

        topBar.getChildren().addAll(menuButton, topBarProfileImage, userInfoBox);
        chatArea.setTop(topBar);

        // Chat area
        chatBox = new VBox(10);
        chatBox.setPadding(new Insets(10));
        chatBox.setStyle("-fx-background-color: #f5f5f5;");
        ScrollPane chatScroll = new ScrollPane(chatBox);
        chatScroll.setFitToWidth(true);
        chatScroll.setFitToHeight(true);
        chatArea.setCenter(chatScroll);

        // Input area
        HBox inputArea = new HBox(10);
        inputArea.setPadding(new Insets(10));
        inputArea.setAlignment(Pos.CENTER);

        TextField messageField = new TextField();
        messageField.setPromptText("Type a message...");
        messageField.setPrefWidth(400);

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage(messageField.getText()));

        messageField.setOnAction(e -> sendMessage(messageField.getText()));

        inputArea.getChildren().addAll(messageField, sendButton);
        chatArea.setBottom(inputArea);

        // Use SplitPane to divide screen into two resizable parts
        splitPane = new SplitPane();
        splitPane.getItems().addAll(contactsScroll, chatArea);
        splitPane.setDividerPositions(0.3);

        // Set the SplitPane as the root of the scene
        root.setCenter(splitPane);

        Scene scene = new Scene(root, 800, 500);
        primaryStage.setTitle("SecureYAC - " + username + " (Port: " + port + ")");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Set up close event handler
        primaryStage.setOnCloseRequest(e -> System.exit(0));
    }

    private void sendMessage(String text) {
        if (currentChat != null && text != null && !text.trim().isEmpty()) {
            String message = text.trim();

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy    HH:mm:ss");
            String timestamp = now.format(formatter);

            // Add the message to history with timestamp
            messageHistory.get(currentChat).add(new String[]{"You", message, timestamp});
            updateChatBox(currentChat);
            updateLastMessage(currentChat, message);

            // Send the message through the server thread
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("username", username);
                jsonObject.put("message", message);
                serverThread.sendMessage(jsonObject.toString());
            } catch (Exception e) {
                showErrorAlert("Error", "Failed to send message", e.getMessage());
            }

            // Clear the message field
            ((TextField)((HBox)((BorderPane)splitPane.getItems().get(1)).getBottom()).getChildren().getFirst()).clear();
        }
    }

    private void addMessage(String sender, String text, String timestamp, boolean isUser) {
        VBox messageContainer = new VBox(2);
        messageContainer.setPadding(new Insets(5));

        Label senderLabel = new Label(isUser ? "You" : sender);
        senderLabel.setStyle("-fx-text-fill: grey; -fx-font-size: 12px;");

        Text messageText = new Text(text);
        messageText.setStyle("-fx-font-size: 14px;");

        Label timestampLabel = new Label(timestamp);
        timestampLabel.setStyle("-fx-text-fill: grey; -fx-font-size: 10px;");

        TextFlow messageBubble = new TextFlow(messageText);
        messageBubble.setMaxWidth(250);
        messageBubble.setPadding(new Insets(10));
        messageBubble.setStyle("-fx-background-radius: 10;");

        if (isUser) {
            messageBubble.setStyle("-fx-background-color: #0084ff; -fx-text-fill: white; -fx-background-radius: 10;");
            messageText.setFill(javafx.scene.paint.Color.WHITE);
            messageContainer.setAlignment(Pos.CENTER_RIGHT);
        } else {
            messageBubble.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: black; -fx-background-radius: 10;");
            messageText.setFill(javafx.scene.paint.Color.BLACK);
            messageContainer.setAlignment(Pos.CENTER_LEFT);
        }

        // Add message and timestamp
        messageContainer.getChildren().addAll(senderLabel, messageBubble, timestampLabel);
        chatBox.getChildren().add(messageContainer);
    }

    private void addContact(String name, String lastMessage, String imageUrl) {
        HBox contactBox = new HBox(10);
        contactBox.setPadding(new Insets(10));
        contactBox.setAlignment(Pos.CENTER_LEFT);
        contactBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d0d0d0; -fx-background-radius: 5; -fx-border-radius: 5;");

        ImageView contactImage = new ImageView(new Image(imageUrl));
        contactImage.setFitWidth(40);
        contactImage.setFitHeight(40);

        VBox textContainer = new VBox();
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold;");
        Label lastMessageLabel = new Label(lastMessage);
        lastMessageLabel.setStyle("-fx-text-fill: grey; -fx-font-size: 12px;");

        textContainer.getChildren().addAll(nameLabel, lastMessageLabel);
        contactBox.getChildren().addAll(contactImage, textContainer);

        // Store references for easy updates
        contactBoxes.put(name, contactBox);
        lastMessageLabels.put(name, lastMessageLabel);

        // Add hover effect
        contactBox.setOnMouseEntered(event -> contactBox.setStyle("-fx-background-color: #d0d0d0; -fx-border-color: #d0d0d0; -fx-background-radius: 5; -fx-border-radius: 5;"));
        contactBox.setOnMouseExited(event -> contactBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d0d0d0; -fx-background-radius: 5; -fx-border-radius: 5;"));

        // Initialize message history if needed
        if (!messageHistory.containsKey(name)) {
            messageHistory.put(name, new ArrayList<>());
        }

        // Add the initial message if there's message history
        if (messageHistory.get(name).isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy    HH:mm:ss");
            String timestamp = now.format(formatter);
            messageHistory.get(name).add(new String[]{name, lastMessage, timestamp});
        }

        contactBox.setOnMouseClicked(event -> {
            currentChat = name;
            updateChatBox(name);
            updateTopBar(name, imageUrl);
        });

        contactsList.getChildren().add(contactBox);
    }

    private void updateTopBar(String name, String imageUrl) {
        topBarUsername.setText(name);
        topBarProfileImage.setImage(new Image(imageUrl));
    }

    private void updateLastMessage(String name, String newMessage) {
        if (lastMessageLabels.containsKey(name)) {
            lastMessageLabels.get(name).setText(newMessage);
        }
    }

    private void updateChatBox(String name) {
        chatBox.getChildren().clear();
        for (String[] message : messageHistory.get(name)) {
            addMessage(message[0], message[1], message[2], message[0].equals("You"));
        }
    }

    private void toggleContacts() {
        if (contactsVisible) {
            splitPane.getItems().removeFirst();
        } else {
            splitPane.getItems().addFirst(contactsScroll);
            splitPane.setDividerPositions(0.3);
        }
        contactsVisible = !contactsVisible;
    }

    private void showAddContact() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add Contact");
        dialog.setHeaderText("Add a new contact");

        // Set the button types
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Create the fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField addressField = new TextField();
        addressField.setPromptText("hostname:port");

        grid.add(new Label("Address:"), 0, 0);
        grid.add(addressField, 1, 0);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the field by default
        Platform.runLater(() -> addressField.requestFocus());

        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return addressField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(addressInfo -> {
            if (!addressInfo.trim().isEmpty()) {
                String[] address = addressInfo.split(":");

                if (address.length == 2) {
                    String host = address[0];
                    String port = address[1];

                    if (!host.trim().isEmpty() && !port.trim().isEmpty()) {
                        String contactId = host + ":" + port;

                        if (!messageHistory.containsKey(contactId)) {
                            // Try to establish connection
                            Socket socket = null;
                            try {
                                socket = new Socket(host, Integer.parseInt(port));
                                PeerThread peerThread = new PeerThread(socket);
                                peerThread.setMessageHandler(this::handleIncomingMessage);
                                peerThread.start();

                                addContact(contactId, "Connected",
                                        "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png");
                            } catch (Exception e) {
                                if (socket != null) {
                                    try {
                                        socket.close();
                                    } catch (IOException ioEx) {
                                        // Ignore
                                    }
                                }

                                // Add the contact anyway, even if connection failed
                                addContact(contactId, "Could not connect",
                                        "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png");

                                showErrorAlert("Connection Error", "Added contact but failed to connect", e.getMessage());
                            }
                        }
                    }
                } else {
                    showErrorAlert("Input Error", "Invalid address format",
                            "Please use the format 'hostname:port'");
                }
            }
        });
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}