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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class UI extends Application {
    private VBox chatBox;
    private VBox contactsList;
    private HashMap<String, List<String[]>> messageHistory;
    private HashMap<String, HBox> contactBoxes;
    private HashMap<String, Label> lastMessageLabels;
    private HashMap<String, Integer> unreadMessages;
    private HashMap<String, StackPane> unreadIndicators;
    private String currentChat;
    private ImageView topBarProfileImage;
    private ImageView userProfileImageView;
    private Label topBarUsername;
    private boolean contactsVisible = true;
    private SplitPane splitPane;
    private ScrollPane contactsScroll;
    private ServerThread serverThread;
    private String username;
    private int port;
    private FileTransferManager fileTransferManager;
    private ExecutorService executorService;
    private Stage primaryStage;
    private String userProfileImagePath = null;
    private String appDataDirectory;

    // Default profile image URL
    private static final String DEFAULT_PROFILE_IMAGE = "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png";


    // Map of peer addresses to sockets to track active connections
    private HashMap<String, PeerThread> activePeers = new HashMap<>();

    // Ports to try, starting from 8000
    private static final AtomicInteger NEXT_PORT = new AtomicInteger(8000);
    private static final int MAX_PORT_ATTEMPTS = 100;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        executorService = Executors.newFixedThreadPool(4);
        fileTransferManager = new FileTransferManager();
        fileTransferManager.setOnFileReceived(this::handleFileReceived);
        fileTransferManager.setOnTransferProgress(this::handleTransferProgress);

        setupAppDataDirectory();

        showLoginDialog(primaryStage);
    }

    // Set up the application data directory to store user profile images
    private void setupAppDataDirectory() {
        appDataDirectory = System.getProperty("user.home") + File.separator + ".SecureYAC";
        try {
            Path appDataPath = Paths.get(appDataDirectory);
            if (!Files.exists(appDataPath)) {
                Files.createDirectories(appDataPath);
            }

            // Create profile images directory
            Path profileImagesPath = Paths.get(appDataDirectory, "ProfileImages");
            if (!Files.exists(profileImagesPath)) {
                Files.createDirectories(profileImagesPath);
            }
        } catch (IOException e) {
            System.err.println("Could not create app data directory: " + e.getMessage());
        }
    }

    private void showLoginDialog(Stage primaryStage) {
        // Create dialog for username and profile picture
        Dialog<LoginData> dialog = new Dialog<>();
        dialog.setTitle("SecureYAC Login");
        dialog.setHeaderText("Welcome to SecureYAC");

        // Set the button types
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the username and profile picture fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        // Profile picture selection
        ImageView profileImageView = new ImageView(new Image(DEFAULT_PROFILE_IMAGE));
        profileImageView.setFitWidth(100);
        profileImageView.setFitHeight(100);

        // Make the ImageView circular
        Circle clip = new Circle(50, 50, 50);
        profileImageView.setClip(clip);

        Button chooseImageButton = new Button("Choose Profile Picture");

        // File path for the selected image
        final File[] selectedImageFile = new File[1];

        chooseImageButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Profile Picture");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );

            File file = fileChooser.showOpenDialog(dialog.getOwner());
            if (file != null) {
                selectedImageFile[0] = file;
                // Load and display the selected image
                Image image = new Image(file.toURI().toString());
                profileImageView.setImage(image);
            }
        });

        VBox imageContainer = new VBox(10);
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.getChildren().addAll(profileImageView, chooseImageButton);

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Profile Picture:"), 0, 1);
        grid.add(imageContainer, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefSize(400, 300);

        // Request focus on the username field by default
        Platform.runLater(usernameField::requestFocus);

        // Convert the result when the login button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new LoginData(usernameField.getText(), selectedImageFile[0]);
            }
            return null;
        });

        Optional<LoginData> result = dialog.showAndWait();

        result.ifPresent(loginData -> {
            if (!loginData.username.isEmpty()) {
                this.username = loginData.username;

                // Handle profile image
                if (loginData.profileImageFile != null) {
                    try {
                        // Copy profile image to app data directory
                        String profileImageName = username + "_profile" + getFileExtension(loginData.profileImageFile.getName());
                        Path destination = Paths.get(appDataDirectory, "ProfileImages", profileImageName);
                        Files.copy(loginData.profileImageFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                        userProfileImagePath = destination.toString();
                    } catch (IOException e) {
                        System.err.println("Could not save profile image: " + e.getMessage());
                        userProfileImagePath = null;
                    }
                }

                // Find an available port automatically
                if (findAvailablePort()) {
                    try {
                        // Start the server thread with the automatically assigned port
                        serverThread = new ServerThread(String.valueOf(port));
                        serverThread.setFileTransferHandler(fileTransferManager::handleIncomingFileTransfer);
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

    // Helper class to store login data
    private static class LoginData {
        String username;
        File profileImageFile;

        public LoginData(String username, File profileImageFile) {
            this.username = username;
            this.profileImageFile = profileImageFile;
        }
    }

    // Helper method to get file extension
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex);
        }
        return ".png"; // Default extension
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
                    connectToPeer(address[0], address[1], peerAddress);
                }
            }
        }
    }

    private void connectToPeer(String host, String portStr, String peerAddress) {
        Socket socket = null;
        try {
            int portNum = Integer.parseInt(portStr);
            socket = new Socket(host, portNum);
            PeerThread peerThread = new PeerThread(socket);
            peerThread.setMessageHandler(this::handleIncomingMessage);
            peerThread.setFileTransferHandler(fileTransferManager::handleIncomingFileTransfer);
            peerThread.start();

            // Store the peer thread for later reference
            activePeers.put(peerAddress, peerThread);

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

                    // Initialise unread counter for new contact if not current chat
                    if (currentChat == null || !currentChat.equals(sender)) {
                        unreadMessages.put(sender, 1);
                        updateUnreadIndicator(sender);
                    }
                } else {
                    // Update the last message
                    messageHistory.get(sender).add(new String[]{sender, msgContent, timestamp});
                    updateLastMessage(sender, msgContent);

                    // Increment unread counter if this isn't the current chat
                    if (currentChat == null || !currentChat.equals(sender)) {
                        int unread = unreadMessages.getOrDefault(sender, 0) + 1;
                        unreadMessages.put(sender, unread);
                        updateUnreadIndicator(sender);
                    }

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
    
    private void updateUnreadIndicator(String contactName) {
        int unread = unreadMessages.getOrDefault(contactName, 0);

        // Get or create the indicator for this contact
        StackPane indicator = unreadIndicators.get(contactName);

        if (indicator == null && unread > 0) {
            // Need to create a new indicator
            createUnreadIndicator(contactName, unread);
        } else if (indicator != null) {
            // Update existing indicator
            if (unread > 0) {
                // Update the count
                Label countLabel = (Label) indicator.getChildren().get(1);
                countLabel.setText(String.valueOf(unread));
                indicator.setVisible(true);
            } else {
                // Hide the indicator when count is 0
                indicator.setVisible(false);
            }
        }
    }

    // Create an unread message indicator for a contact
    private void createUnreadIndicator(String contactName, int count) {
        HBox contactBox = contactBoxes.get(contactName);
        if (contactBox != null) {
            // Create indicator components
            Circle circle = new Circle(10);
            circle.setFill(Color.web("#FF3B30"));

            Label countLabel = new Label(String.valueOf(count));
            countLabel.setTextFill(Color.WHITE);
            countLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10px;");

            // Create stack pane to overlay text on circle
            StackPane indicator = new StackPane();
            indicator.getChildren().addAll(circle, countLabel);
            indicator.setAlignment(Pos.CENTER);

            // Add the indicator to the contact box
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            contactBox.getChildren().add(spacer);
            contactBox.getChildren().add(indicator);

            // Store reference to the indicator
            unreadIndicators.put(contactName, indicator);
        }
    }

    private void handleFileReceived(String message) {
        Platform.runLater(() -> {
            // Add the file received confirmation as a system message
            addSystemMessage(message);

            // Create an alert to notify the user
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("File Received");
            alert.setHeaderText("File Transfer Complete");
            alert.setContentText(message + "\nSaved to: " + fileTransferManager.getDownloadDirectory());

            // Add a button to open the download directory
            ButtonType openFolderButton = new ButtonType("Open Folder");
            alert.getButtonTypes().setAll(openFolderButton, ButtonType.OK);

            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == openFolderButton) {
                    try {
                        // Open the download directory using the system file explorer
                        java.awt.Desktop.getDesktop().open(new File(fileTransferManager.getDownloadDirectory()));
                    } catch (IOException e) {
                        showErrorAlert("Error", "Could not open download directory", e.getMessage());
                    }
                }
            });
        });
    }

    private void handleTransferProgress(String message) {
        Platform.runLater(() -> {
            // Show progress in the current chat
            if (currentChat != null) {
                updateSystemMessage(message);
            }
        });
    }

    private void addSystemMessage(String text) {
        if (currentChat != null) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy    HH:mm:ss");
            String timestamp = now.format(formatter);

            messageHistory.get(currentChat).add(new String[]{"SYSTEM", text, timestamp});
            updateChatBox(currentChat);
        }
    }

    // Map to track file transfer message indices in the message history
    private HashMap<String, Integer> fileTransferMessageIndices = new HashMap<>();

    private void updateSystemMessage(String text) {
        // Find the latest system message and update it (for progress updates)
        if (currentChat != null) {
            List<String[]> messages = messageHistory.get(currentChat);
            String transferKey = "";

            // Determine the key for this transfer (sender + filename)
            if (text.startsWith("Receiving file from")) {
                // Extract sender and filename from message
                // Format: "Receiving file from [sender]: [filename] ([percentage]%)"
                String[] parts = text.split(":");
                if (parts.length >= 2) {
                    String sender = parts[0].replace("Receiving file from ", "").trim();
                    String fileInfo = parts[1].trim();
                    String filename = fileInfo.split("\\(")[0].trim();
                    transferKey = sender + "-" + filename;

                    // Check if this transfer is complete
                    if (text.contains("(100%)")) {
                        // If complete, remove from tracking and let the complete message appear
                        if (fileTransferMessageIndices.containsKey(transferKey)) {
                            int index = fileTransferMessageIndices.get(transferKey);
                            if (index < messages.size()) {
                                // Remove the progress message
                                messages.remove(index);
                            }
                            fileTransferMessageIndices.remove(transferKey);
                        }
                        return;
                    }
                }
            } else if (text.startsWith("Sending file:")) {
                // Extract filename from message
                // Format: "Sending file: [filename] ([percentage]%)"
                String[] parts = text.split(":");
                if (parts.length >= 2) {
                    String fileInfo = parts[1].trim();
                    String filename = fileInfo.split("\\(")[0].trim();
                    transferKey = "sending-" + filename;

                    // Check if this transfer is complete
                    if (text.contains("(100%)")) {
                        // If complete, remove from tracking and let the complete message appear
                        if (fileTransferMessageIndices.containsKey(transferKey)) {
                            int index = fileTransferMessageIndices.get(transferKey);
                            if (index < messages.size()) {
                                // Remove the progress message
                                messages.remove(index);
                            }
                            fileTransferMessageIndices.remove(transferKey);
                        }
                        return;
                    }
                }
            }

            // If this is a tracked file transfer, update the existing message
            if (!transferKey.isEmpty() && fileTransferMessageIndices.containsKey(transferKey)) {
                int index = fileTransferMessageIndices.get(transferKey);
                if (index < messages.size()) {
                    messages.get(index)[1] = text;
                    updateChatBox(currentChat);
                    return;
                }
            }

            // If we get here, this is a new file transfer or the tracked message was lost
            if (!transferKey.isEmpty()) {
                // Add a new system message and track its index
                addSystemMessage(text);
                fileTransferMessageIndices.put(transferKey, messages.size() - 1);
            } else {
                // Regular system message, not a file transfer
                addSystemMessage(text);
            }
        }
    }

    private void initMainUI(Stage primaryStage) {
        // Main layout using BorderPane
        BorderPane root = new BorderPane();
        messageHistory = new HashMap<>();
        contactBoxes = new HashMap<>();
        lastMessageLabels = new HashMap<>();
        unreadMessages = new HashMap<>();
        unreadIndicators = new HashMap<>();

        // Left sidebar with contacts list
        BorderPane contactsPane = new BorderPane();
        contactsPane.setStyle("-fx-background-color: #e0e0e0;");

        // Top area of contacts pane
        HBox contactsHeader = new HBox(10);
        contactsHeader.setPadding(new Insets(10));
        contactsHeader.setAlignment(Pos.CENTER_LEFT);

        // Add contact button
        Button addContactButton = new Button("+");
        addContactButton.setStyle("-fx-font-size: 18px; -fx-background-color: transparent;");
        addContactButton.setOnAction(e -> showAddContact());

        Label contactsHeading = new Label("Contacts");
        contactsHeading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        contactsHeader.getChildren().addAll(addContactButton, contactsHeading);
        contactsPane.setTop(contactsHeader);

        contactsList = new VBox(10);
        contactsList.setPadding(new Insets(10));

        ScrollPane contactsListScroll = new ScrollPane(contactsList);
        contactsListScroll.setFitToWidth(true);
        contactsListScroll.setFitToHeight(true);
        contactsPane.setCenter(contactsListScroll);

        // Bottom area of contacts pane with user profile and settings
        HBox userProfileArea = new HBox(10);
        userProfileArea.setPadding(new Insets(10));
        userProfileArea.setAlignment(Pos.CENTER_LEFT);
        userProfileArea.setStyle("-fx-background-color: #d0d0d0; -fx-background-radius: 5;");

        // User profile image
        userProfileImageView = new ImageView();
        userProfileImageView.setFitWidth(40);
        userProfileImageView.setFitHeight(40);

        // Make the profile image circular
        Circle profileClip = new Circle(20, 20, 20);
        userProfileImageView.setClip(profileClip);

        // Set the profile image
        if (userProfileImagePath != null) {
            userProfileImageView.setImage(new Image("file:" + userProfileImagePath));
        } else {
            userProfileImageView.setImage(new Image(DEFAULT_PROFILE_IMAGE));
        }

        // User info
        VBox userInfo = new VBox(2);
        Label usernameLabel = new Label(username);
        usernameLabel.setStyle("-fx-font-weight: bold;");
        Label portLabel = new Label("Port: " + port);
        portLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: grey;");
        userInfo.getChildren().addAll(usernameLabel, portLabel);

        // Settings button
        Button settingsButton = new Button("⚙");
        settingsButton.setStyle("-fx-font-size: 18px; -fx-background-color: transparent;");
        settingsButton.setOnAction(e -> showPeerConnectionDialog());

        // Change profile picture button
        Button changeProfileButton = new Button("📷");
        changeProfileButton.setStyle("-fx-font-size: 18px; -fx-background-color: transparent;");
        changeProfileButton.setOnAction(e -> changeProfilePicture());

        // Add spacing to push settings button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        userProfileArea.getChildren().addAll(userProfileImageView, userInfo, spacer, changeProfileButton, settingsButton);
        contactsPane.setBottom(userProfileArea);

        contactsScroll = new ScrollPane(contactsPane);
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

        topBarProfileImage = new ImageView(new Image(DEFAULT_PROFILE_IMAGE));
        topBarProfileImage.setFitWidth(50);
        topBarProfileImage.setFitHeight(50);

        // Make the top bar profile image circular
        Circle topBarClip = new Circle(25, 25, 25);
        topBarProfileImage.setClip(topBarClip);

        topBarUsername = new Label("Select a Contact");
        topBarUsername.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // User status label (showing current username)
        Label userStatusLabel = new Label("Logged in as: " + username);
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
        chatScroll.vvalueProperty().bind(chatBox.heightProperty());
        chatArea.setCenter(chatScroll);

        // Input area
        VBox inputContainer = new VBox(5);
        inputContainer.setPadding(new Insets(10));

        // Message input field and send button
        HBox inputArea = new HBox(10);
        inputArea.setAlignment(Pos.CENTER);

        TextField messageField = new TextField();
        messageField.setPromptText("Type a message...");
        messageField.setPrefWidth(400);
        HBox.setHgrow(messageField, Priority.ALWAYS);

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage(messageField.getText()));

        messageField.setOnAction(e -> sendMessage(messageField.getText()));

        // File attachment button
        Button attachButton = new Button("📎");
        attachButton.setTooltip(new Tooltip("Send File"));
        attachButton.setStyle("-fx-font-size: 18px; -fx-background-color: transparent;");
        attachButton.setOnAction(e -> sendFile());

        inputArea.getChildren().addAll(messageField, attachButton, sendButton);

        // Add a file transfer progress label
        Label fileTransferLabel = new Label("");
        fileTransferLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: grey;");

        inputContainer.getChildren().addAll(inputArea, fileTransferLabel);
        chatArea.setBottom(inputContainer);

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
        primaryStage.setOnCloseRequest(e -> {
            executorService.shutdown();
            System.exit(0);
        });
    }

    // Method to change profile picture
    private void changeProfilePicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select New Profile Picture");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                // Copy profile image to app data directory
                String profileImageName = username + "_profile" + getFileExtension(file.getName());
                Path destination = Paths.get(appDataDirectory, "ProfileImages", profileImageName);
                Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                userProfileImagePath = destination.toString();

                // Update profile image in UI
                Image newImage = new Image("file:" + userProfileImagePath);
                userProfileImageView.setImage(newImage);
            } catch (IOException e) {
                showErrorAlert("Error", "Could not save profile image", e.getMessage());
            }
        }
    }

    private void sendFile() {
        if (currentChat == null) {
            showErrorAlert("Error", "No Contact Selected", "Please select a contact to send a file to.");
            return;
        }

        // Open file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Send");
        File selectedFile = fileChooser.showOpenDialog(primaryStage);

        if (selectedFile != null) {
            // Check file size
            long fileSize = selectedFile.length();

            if (fileSize > 100 * 1024 * 1024) { // 100 MB limit
                showErrorAlert("Error", "File Too Large",
                        "The selected file is too large. Maximum size is 100 MB.");
                return;
            }

            // Show confirmation dialog
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Send File");
            confirmDialog.setHeaderText("Send " + selectedFile.getName() + " to " + currentChat + "?");
            confirmDialog.setContentText("File size: " + formatFileSize(fileSize));

            Optional<ButtonType> result = confirmDialog.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Start file transfer in a background thread
                executorService.submit(() -> {
                    try {
                        // Prepare file transfer messages
                        JSONObject[] fileMessages = fileTransferManager.prepareFileTransfer(username, selectedFile);

                        // Update UI with initial progress message
                        final String fileName = selectedFile.getName();
                        Platform.runLater(() -> updateSystemMessage("Sending file: " + fileName + " (0%)"));

                        // Send each chunk with a small delay to prevent overwhelming the network
                        for (int i = 0; i < fileMessages.length; i++) {
                            final int progress = (i + 1) * 100 / fileMessages.length;
                            serverThread.sendMessage(fileMessages[i].toString());

                            // Update progress every 5% or for the initial update
                            if (progress % 5 == 0 || i == 0) {
                                Platform.runLater(() -> updateSystemMessage("Sending file: " + fileName + " (" + progress + "%)"));
                            }

                            // Send the final progress update for 100%
                            if (i == fileMessages.length - 1) {
                                Platform.runLater(() -> updateSystemMessage("Sending file: " + fileName + " (100%)"));
                            }

                            if (fileMessages.length > 1) {
                                Thread.sleep(50); // Small delay between chunks
                            }
                        }

                        // File transfer complete
                        Platform.runLater(() -> addSystemMessage("File sent: " + selectedFile.getName()));

                    } catch (Exception e) {
                        Platform.runLater(() -> showErrorAlert("Error", "Failed to Send File", e.getMessage()));
                    }
                });
            }
        }
    }

    private String formatFileSize(long size) {
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int unitIndex = 0;
        double unitValue = size;

        while (unitValue > 1024 && unitIndex < units.length - 1) {
            unitValue /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", unitValue, units[unitIndex]);
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
            TextField messageField = (TextField)((HBox)((VBox)((BorderPane)splitPane.getItems().get(1)).getBottom()).getChildren().getFirst()).getChildren().getFirst();
            messageField.clear();
        }
    }

    private void addMessage(String sender, String text, String timestamp, boolean isUser) {
        VBox messageContainer = new VBox(2);
        messageContainer.setPadding(new Insets(5));

        // Message label
        Label senderLabel = new Label(isUser ? "You" : sender);
        senderLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px; -fx-font-weight: bold;");

        // Text content
        Text messageText = new Text(text);
        messageText.setStyle("-fx-font-size: 14px;");

        // Timestamp
        Label timestampLabel = new Label(timestamp);
        timestampLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 10px;");

        // Message bubble
        TextFlow messageBubble = new TextFlow(messageText);
        messageBubble.setPadding(new Insets(12, 15, 12, 15));

        if (isUser) {
            // User messages
            messageBubble.setStyle(
                    "-fx-background-color: #4DA6FF; " +
                            "-fx-background-radius: 18 18 0 18; " + // Rounded corners with one sharp corner
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 1, 1, 0, 1);"
            );
            messageText.setFill(javafx.scene.paint.Color.WHITE);
            messageContainer.setAlignment(Pos.CENTER_RIGHT);
            timestampLabel.setAlignment(Pos.CENTER_RIGHT);
            VBox.setMargin(timestampLabel, new Insets(0, 5, 0, 0));
        } else if ("SYSTEM".equals(sender)) {
            // System messages
            messageBubble.setStyle(
                    "-fx-background-color: #F0F2F5; " +
                            "-fx-background-radius: 18; " +
                            "-fx-border-color: #E4E6EB; " +
                            "-fx-border-radius: 18; " +
                            "-fx-border-width: 1px;"
            );
            messageText.setFill(javafx.scene.paint.Color.web("#666666"));
            messageContainer.setAlignment(Pos.CENTER);
            senderLabel.setText(""); // No sender for system messages
        } else {
            // Others' messages
            messageBubble.setStyle(
                    "-fx-background-color: #E4E6EB; " +
                            "-fx-background-radius: 18 18 18 0; " + // Rounded corners with one sharp corner
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 1, 1, 0, 1);"
            );
            messageText.setFill(javafx.scene.paint.Color.web("#333333"));
            messageContainer.setAlignment(Pos.CENTER_LEFT);
        }

        // Set max width but allow breaking for long messages
        messageBubble.setMaxWidth(300);

        // Add components to the message container
        if ("SYSTEM".equals(sender)) {
            messageContainer.getChildren().addAll(messageBubble, timestampLabel);
        } else {
            messageContainer.getChildren().addAll(senderLabel, messageBubble, timestampLabel);
        }

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

        // Initialise unread messages counter if not already set
        if (!unreadMessages.containsKey(name)) {
            unreadMessages.put(name, 0);
        }

        // Create unread indicator if needed
        int unread = unreadMessages.getOrDefault(name, 0);
        if (unread > 0) {
            createUnreadIndicator(name, unread);
        }

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
            // Reset unread counter when chat is opened
            unreadMessages.put(name, 0);
            updateUnreadIndicator(name);

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
        Platform.runLater(addressField::requestFocus);

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
                            connectToPeer(host, port, contactId);
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