package ie.dcu.secureYAC;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
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
    private HBox selectedContactBox = null;
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

    private static final String APP_ICON_PATH = "/secureYAC_icon_256.png";
    private Image appIcon = new Image(APP_ICON_PATH);

    private static final String APP_LOGO_PATH = "/secureYAC_logo.png";

    // Map of peer addresses to sockets to track active connections
    private final HashMap<String, PeerThread> activePeers = new HashMap<>();

    private final HashMap<String, ContactActivity> contactActivities = new HashMap<>();

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

        primaryStage.getIcons().add(appIcon);

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
        Stage dialogStage = new Stage();
        dialogStage.initOwner(primaryStage);
        dialogStage.setTitle("Welcome to SecureYAC");
        dialogStage.setResizable(false);
        dialogStage.getIcons().add(appIcon);

        // Create the main container
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(30));
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa, #e4e7eb);");

        // Title section with logo
        VBox titleBox = new VBox(10);
        titleBox.setAlignment(Pos.CENTER);

        try {
            // Try to load the logo image
            ImageView logoImageView = new ImageView(new Image(APP_LOGO_PATH));
            logoImageView.setFitWidth(180); // Adjust size as needed
            logoImageView.setPreserveRatio(true);

            Label subTitle = new Label("The Most Secure Chat");
            subTitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 0 0 20 0;");

            titleBox.getChildren().addAll(logoImageView, subTitle);
        } catch (Exception e) {
            // Fallback to text if image loading fails
            System.err.println("Could not load application logo: " + e.getMessage());

            Label appTitle = new Label("SecureYAC");
            appTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #3498db;");

            Label subTitle = new Label("The Most Secure Chat");
            subTitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 0 0 20 0;");

            titleBox.getChildren().addAll(appTitle, subTitle);
        }

        // Profile picture container
        StackPane imageContainer = new StackPane();
        imageContainer.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);");

        // Profile picture selection
        ImageView profileImageView = new ImageView(new Image(APP_ICON_PATH));
        profileImageView.setFitWidth(120);
        profileImageView.setFitHeight(120);

        // Make the ImageView circular
        Circle clip = new Circle(60, 60, 60);
        profileImageView.setClip(clip);

        // Border around the image
        Circle border = new Circle(61, 61, 61);
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.web("#3498db"));
        border.setStrokeWidth(2);

        imageContainer.getChildren().addAll(profileImageView, border);

        // Camera overlay for image selection
        StackPane cameraOverlay = new StackPane();
        Circle cameraCircle = new Circle(25);
        cameraCircle.setFill(Color.web("#3498db"));
        Label cameraIcon = new Label("📷");
        cameraIcon.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        cameraOverlay.getChildren().addAll(cameraCircle, cameraIcon);
        cameraOverlay.setTranslateX(40);
        cameraOverlay.setTranslateY(40);
        cameraOverlay.setPickOnBounds(false);
        cameraOverlay.setCursor(Cursor.HAND);

        StackPane profilePictureStack = new StackPane();
        profilePictureStack.getChildren().addAll(imageContainer, cameraOverlay);

        // File path for the selected image
        final File[] selectedImageFile = new File[1];

        cameraOverlay.setOnMouseClicked(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Profile Picture");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );

            File file = fileChooser.showOpenDialog(dialogStage);
            if (file != null) {
                selectedImageFile[0] = file;
                // Load and display the selected image
                Image image = new Image(file.toURI().toString(), 120, 120, true, true);
                profileImageView.setImage(image);
            }
        });

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setPrefHeight(40);
        usernameField.setMaxWidth(300);
        usernameField.setStyle("-fx-background-radius: 20; -fx-font-size: 14px; -fx-padding: 10;" +
                "-fx-border-color: #3498db; -fx-border-radius: 20; -fx-border-width: 2px;");

        Button loginButton = new Button("Start Chatting");
        loginButton.setPrefHeight(40);
        loginButton.setPrefWidth(300);
        loginButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #3498db, #2980b9); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-background-radius: 20; " +
                        "-fx-cursor: hand;"
        );

        // Hover effect for login button
        loginButton.setOnMouseEntered(e ->
                loginButton.setStyle(
                        "-fx-background-color: linear-gradient(to right, #2980b9, #3498db); " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 14px; " +
                                "-fx-background-radius: 20; " +
                                "-fx-cursor: hand;"
                )
        );

        loginButton.setOnMouseExited(e ->
                loginButton.setStyle(
                        "-fx-background-color: linear-gradient(to right, #3498db, #2980b9); " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 14px; " +
                                "-fx-background-radius: 20; " +
                                "-fx-cursor: hand;"
                )
        );

        mainContainer.getChildren().addAll(
                titleBox,
                profilePictureStack,
                new Label("Choose a username to identify yourself"),
                usernameField,
                loginButton
        );

        // Handle login button action
        loginButton.setOnAction(e -> {
            String enteredUsername = usernameField.getText().trim();
            if (enteredUsername.isEmpty()) {
                // Show error for empty username
                usernameField.setStyle(
                        "-fx-background-radius: 20; -fx-font-size: 14px; -fx-padding: 10; " +
                                "-fx-border-color: #e74c3c; -fx-border-radius: 20; -fx-border-width: 2px;"
                );
                return;
            }

            // Process login
            username = enteredUsername;

            // Handle profile image
            if (selectedImageFile[0] != null) {
                try {
                    // Copy profile image to app data directory
                    String profileImageName = username + "_profile" + getFileExtension(selectedImageFile[0].getName());
                    Path destination = Paths.get(appDataDirectory, "ProfileImages", profileImageName);
                    Files.copy(selectedImageFile[0].toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                    userProfileImagePath = destination.toString();
                } catch (IOException ex) {
                    System.err.println("Could not save profile image: " + ex.getMessage());
                    userProfileImagePath = null;
                }
            }

            dialogStage.close();

            // Continue with port finding and connecting
            if (findAvailablePort()) {
                try {
                    // Start the server thread with the automatically assigned port
                    serverThread = new ServerThread(String.valueOf(port));
                    serverThread.setFileTransferHandler(fileTransferManager::handleIncomingFileTransfer);
                    serverThread.start();

                    // Now show the main UI
                    initMainUI(primaryStage);

                } catch (IOException ex) {
                    showErrorAlert("Error", "Could not start server on port " + port, ex.getMessage());
                }
            } else {
                showErrorAlert("Error", "Could not find an available port",
                        "Tried " + MAX_PORT_ATTEMPTS + " ports starting from " + NEXT_PORT.get() +
                                " but none were available.");
            }
        });

        // Allow pressing Enter on the username field to login
        usernameField.setOnAction(loginButton.getOnAction());

        Scene dialogScene = new Scene(mainContainer, 400, 500);
        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait();
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
                    addContact(peerAddress, "Connected", APP_ICON_PATH);

                    // New contact - add to activity tracker
                    contactActivities.put(peerAddress, new ContactActivity(peerAddress, LocalDateTime.now()));
                    sortContactsByActivity();
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
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
                String timestamp = now.format(formatter);

                // Update contact activity time
                contactActivities.put(sender, new ContactActivity(sender, now));

                // If the sender is not in our contacts, add them
                if (!messageHistory.containsKey(sender)) {
                    addContact(sender, msgContent, APP_ICON_PATH);

                    // Initialize unread counter for new contact if not current chat
                    if (currentChat == null || !currentChat.equals(sender)) {
                        unreadMessages.put(sender, 1);
                        updateUnreadIndicator(sender);
                    }
                } else {
                    // Update the last message
                    messageHistory.get(sender).add(new String[]{sender, msgContent, timestamp});
                    updateLastMessage(sender, msgContent);
                    updateLastMessageTimestamp(sender, timestamp);

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

                // Sort contacts by activity time
                sortContactsByActivity();

            } catch (Exception e) {
                System.err.println("Error processing incoming message: " + e.getMessage());
            }
        });
    }
    
    private void updateUnreadIndicator(String contactName) {
        int unread = unreadMessages.getOrDefault(contactName, 0);

        // Get or create the indicator for this contact
        StackPane indicator = unreadIndicators.get(contactName);

        if (indicator == null) return;

        // Clear previous indicator content
        indicator.getChildren().clear();

        if (unread > 0) {
            HBox indicatorContainer = new HBox();
            indicatorContainer.setAlignment(Pos.CENTER_RIGHT);

            Circle circle = new Circle(10);
            circle.setFill(Color.web("#3498db"));

            Label countLabel = new Label(String.valueOf(unread));
            countLabel.setTextFill(Color.WHITE);
            countLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10px;");

            StackPane badgeStack = new StackPane(circle, countLabel);
            indicatorContainer.getChildren().add(badgeStack);

            indicator.getChildren().add(indicatorContainer);
            indicator.setVisible(true);

            // Add subtle animation for new messages
            circle.setScaleX(0);
            circle.setScaleY(0);

            Timeline timeline = new Timeline();
            KeyValue kv1 = new KeyValue(circle.scaleXProperty(), 1.2);
            KeyValue kv2 = new KeyValue(circle.scaleYProperty(), 1.2);
            KeyFrame kf1 = new KeyFrame(Duration.millis(150), kv1, kv2);

            KeyValue kv3 = new KeyValue(circle.scaleXProperty(), 1);
            KeyValue kv4 = new KeyValue(circle.scaleYProperty(), 1);
            KeyFrame kf2 = new KeyFrame(Duration.millis(200), kv3, kv4);

            timeline.getKeyFrames().addAll(kf1, kf2);
            timeline.play();
        } else {
            // Hide the indicator when count is 0
            indicator.setVisible(false);
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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
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
        contactsPane.setStyle("-fx-background-color: white;");

        // Top area of contacts pane
        HBox contactsHeader = new HBox(15);
        contactsHeader.setPadding(new Insets(20, 15, 20, 15));
        contactsHeader.setAlignment(Pos.CENTER_LEFT);
        contactsHeader.setStyle("-fx-border-color: transparent transparent #f2f2f2 transparent; -fx-border-width: 0 0 1 0;");

        // Add contact button
        Button addContactButton = new Button("+");
        addContactButton.setStyle(
                "-fx-font-size: 18px; " +
                        "-fx-background-color: #3498db; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 50%; " +
                        "-fx-min-width: 38px; " +
                        "-fx-min-height: 38px; " +
                        "-fx-max-width: 38px; " +
                        "-fx-max-height: 38px; " +
                        "-fx-cursor: hand;"
        );

        // Hover effect for add contact button
        addContactButton.setOnMouseEntered(e ->
                addContactButton.setStyle(
                        "-fx-font-size: 18px; " +
                                "-fx-background-color: #2980b9; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 50%; " +
                                "-fx-min-width: 38px; " +
                                "-fx-min-height: 38px; " +
                                "-fx-max-width: 38px; " +
                                "-fx-max-height: 38px; " +
                                "-fx-cursor: hand;"
                )
        );

        addContactButton.setOnMouseExited(e ->
                addContactButton.setStyle(
                        "-fx-font-size: 18px; " +
                                "-fx-background-color: #3498db; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 50%; " +
                                "-fx-min-width: 38px; " +
                                "-fx-min-height: 38px; " +
                                "-fx-max-width: 38px; " +
                                "-fx-max-height: 38px; " +
                                "-fx-cursor: hand;"
                )
        );

        addContactButton.setOnAction(e -> showAddContact());

        Label contactsHeading = new Label("Contacts");
        contactsHeading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        contactsHeader.getChildren().addAll(addContactButton, contactsHeading);
        contactsPane.setTop(contactsHeader);

        // Create a search box for contacts
        TextField searchField = new TextField();
        searchField.setPromptText("Search contacts...");
        searchField.setStyle(
                "-fx-background-color: #f2f2f2; " +
                        "-fx-background-radius: 20px; " +
                        "-fx-padding: 8 15 8 15; " +
                        "-fx-font-size: 14px;"
        );

        HBox searchBox = new HBox(searchField);
        searchBox.setPadding(new Insets(0, 15, 15, 15));
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // Add search functionality to filter contacts
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterContacts(newValue));

        // Create a VBox to hold search and contacts list
        VBox contactsWithSearch = new VBox(10);
        contactsWithSearch.getChildren().add(searchBox);

        contactsList = new VBox(10);
        contactsList.setPadding(new Insets(5, 15, 15, 15));
        contactsList.setStyle("-fx-background-color: transparent;");

        contactsWithSearch.getChildren().add(contactsList);

        ScrollPane contactsListScroll = new ScrollPane(contactsWithSearch);
        contactsListScroll.setFitToWidth(true);
        contactsListScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        contactsListScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contactsPane.setCenter(contactsListScroll);

        // Bottom area of contacts pane with user profile and settings
        HBox userProfileArea = new HBox(15);
        userProfileArea.setPadding(new Insets(15));
        userProfileArea.setAlignment(Pos.CENTER_LEFT);
        userProfileArea.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #f2f2f2 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        // User profile image - use custom if available
        userProfileImageView = new ImageView();
        userProfileImageView.setFitWidth(45);
        userProfileImageView.setFitHeight(45);

        // Make the profile image circular
        Circle profileClip = new Circle(22.5, 22.5, 22.5);
        userProfileImageView.setClip(profileClip);

        // Container for profile image with border
        StackPane profileImageContainer = new StackPane();
        Circle imageBorder = new Circle(23, 23, 23);
        imageBorder.setFill(Color.TRANSPARENT);
        imageBorder.setStroke(Color.web("#3498db"));
        imageBorder.setStrokeWidth(2);

        // Set the profile image
        if (userProfileImagePath != null) {
            userProfileImageView.setImage(new Image("file:" + userProfileImagePath));
        } else {
            userProfileImageView.setImage(new Image(APP_ICON_PATH));
        }

        profileImageContainer.getChildren().addAll(userProfileImageView, imageBorder);

        // User info
        VBox userInfo = new VBox(2);
        Label usernameLabel = new Label(username);
        usernameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e; -fx-font-size: 14px;");
        Label portLabel = new Label("Port: " + port);
        portLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");
        userInfo.getChildren().addAll(usernameLabel, portLabel);

        // Settings button
        Button settingsButton = new Button("⚙");
        settingsButton.setMinWidth(40);
        settingsButton.setStyle(
                "-fx-font-size: 18px; " +
                        "-fx-background-color: transparent; " +
                        "-fx-text-fill: #7f8c8d; " +
                        "-fx-cursor: hand;"
        );
        settingsButton.setOnMouseEntered(e ->
                settingsButton.setStyle(
                        "-fx-font-size: 18px; " +
                                "-fx-background-color: #f2f2f2; " +
                                "-fx-text-fill: #34495e; " +
                                "-fx-background-radius: 50%; " +
                                "-fx-cursor: hand;"
                )
        );
        settingsButton.setOnMouseExited(e ->
                settingsButton.setStyle(
                        "-fx-font-size: 18px; " +
                                "-fx-background-color: transparent; " +
                                "-fx-text-fill: #7f8c8d; " +
                                "-fx-cursor: hand;"
                )
        );
        settingsButton.setOnAction(e -> {});

        // Change profile picture button
        Button changeProfileButton = new Button("📷");
        changeProfileButton.setTooltip(new Tooltip("Change Profile Picture"));
        changeProfileButton.setMinWidth(40);
        changeProfileButton.setStyle(
                "-fx-font-size: 16px; " +
                        "-fx-background-color: transparent; " +
                        "-fx-text-fill: #7f8c8d; " +
                        "-fx-cursor: hand;"
        );
        changeProfileButton.setOnMouseEntered(e ->
                changeProfileButton.setStyle(
                        "-fx-font-size: 16px; " +
                                "-fx-background-color: #f2f2f2; " +
                                "-fx-text-fill: #34495e; " +
                                "-fx-background-radius: 50%; " +
                                "-fx-cursor: hand;"
                )
        );
        changeProfileButton.setOnMouseExited(e ->
                changeProfileButton.setStyle(
                        "-fx-font-size: 16px; " +
                                "-fx-background-color: transparent; " +
                                "-fx-text-fill: #7f8c8d; " +
                                "-fx-cursor: hand;"
                )
        );
        changeProfileButton.setOnAction(e -> changeProfilePicture());

        // Add spacing to push settings button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        userProfileArea.getChildren().addAll(profileImageContainer, userInfo, spacer, changeProfileButton, settingsButton);
        contactsPane.setBottom(userProfileArea);

        contactsScroll = new ScrollPane(contactsPane);
        contactsScroll.setFitToWidth(true);
        contactsScroll.setFitToHeight(true);
        contactsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Chat area
        BorderPane chatArea = new BorderPane();
        chatArea.setStyle("-fx-background-color: #f5f7fa;");

        // Top bar with the person we are chatting with
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #f2f2f2 transparent; -fx-border-width: 0 0 1 0;");

        Button menuButton = new Button("☰");
        menuButton.setStyle(
                "-fx-font-size: 18px; " +
                        "-fx-background-color: transparent; " +
                        "-fx-text-fill: #7f8c8d; " +
                        "-fx-cursor: hand;"
        );
        menuButton.setOnMouseEntered(e ->
                menuButton.setStyle(
                        "-fx-font-size: 18px; " +
                                "-fx-background-color: #f2f2f2; " +
                                "-fx-text-fill: #34495e; " +
                                "-fx-background-radius: 50%; " +
                                "-fx-cursor: hand;"
                )
        );
        menuButton.setOnMouseExited(e ->
                menuButton.setStyle(
                        "-fx-font-size: 18px; " +
                                "-fx-background-color: transparent; " +
                                "-fx-text-fill: #7f8c8d; " +
                                "-fx-cursor: hand;"
                )
        );
        menuButton.setOnAction(e -> toggleContacts());

        // Contact profile image with border
        topBarProfileImage = new ImageView(new Image(APP_ICON_PATH));
        topBarProfileImage.setFitWidth(50);
        topBarProfileImage.setFitHeight(50);

        // Make the top bar profile image circular
        Circle topBarClip = new Circle(25, 25, 25);
        topBarProfileImage.setClip(topBarClip);

        // Container for contact image with border
        StackPane contactImageContainer = new StackPane();
        Circle contactImageBorder = new Circle(25.5, 25.5, 25.5);
        contactImageBorder.setFill(Color.TRANSPARENT);
        contactImageBorder.setStroke(Color.web("#3498db"));
        contactImageBorder.setStrokeWidth(1);
        contactImageContainer.getChildren().addAll(topBarProfileImage, contactImageBorder);

        topBarUsername = new Label("Select a Contact");
        topBarUsername.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        // User status label (showing current username)
        Label userStatusLabel = new Label("Logged in as: " + username);
        userStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");

        VBox userInfoBox = new VBox(5);
        userInfoBox.getChildren().addAll(topBarUsername, userStatusLabel);

        topBar.getChildren().addAll(menuButton, contactImageContainer, userInfoBox);
        chatArea.setTop(topBar);

        // Chat area
        chatBox = new VBox(10);
        chatBox.setPadding(new Insets(15));

        ScrollPane chatScroll = new ScrollPane(chatBox);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        chatScroll.vvalueProperty().bind(chatBox.heightProperty());
        chatArea.setCenter(chatScroll);

        // Input area
        VBox inputContainer = new VBox(5);
        inputContainer.setPadding(new Insets(15));
        inputContainer.setStyle("-fx-background-color: white; -fx-border-color: #f2f2f2 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        // Message input field and send button
        HBox inputArea = new HBox(10);
        inputArea.setAlignment(Pos.CENTER);

        TextField messageField = new TextField();
        messageField.setPromptText("Type a message...");
        messageField.setPrefHeight(40);
        messageField.setStyle(
                "-fx-background-color: #f5f7fa; " +
                        "-fx-background-radius: 20px; " +
                        "-fx-padding: 0 15 0 15; " +
                        "-fx-font-size: 14px;"
        );
        HBox.setHgrow(messageField, Priority.ALWAYS);

        Button sendButton = new Button("Send");
        sendButton.setPrefHeight(42);
        sendButton.setMinWidth(42);
        sendButton.setStyle(
                "-fx-background-color: #3498db; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 20px; " +
                        "-fx-cursor: hand;"
        );
        sendButton.setOnMouseEntered(e ->
                sendButton.setStyle(
                        "-fx-background-color: #2980b9; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-weight: bold; " +
                                "-fx-background-radius: 20px; " +
                                "-fx-cursor: hand;"
                )
        );
        sendButton.setOnMouseExited(e ->
                sendButton.setStyle(
                        "-fx-background-color: #3498db; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-weight: bold; " +
                                "-fx-background-radius: 20px; " +
                                "-fx-cursor: hand;"
                )
        );
        sendButton.setOnAction(e -> sendMessage(messageField.getText()));

        messageField.setOnAction(e -> sendMessage(messageField.getText()));

        // File attachment button
        Button attachButton = new Button("📎");
        attachButton.setTooltip(new Tooltip("Send File"));
        attachButton.setPrefHeight(42);
        attachButton.setMinWidth(42);
        attachButton.setStyle(
                "-fx-font-size: 18px; " +
                        "-fx-background-color: #f5f7fa; " +
                        "-fx-background-radius: 20px; " +
                        "-fx-cursor: hand;"
        );
        attachButton.setOnMouseEntered(e ->
                attachButton.setStyle(
                        "-fx-font-size: 18px; " +
                                "-fx-background-color: #e0e5ea; " +
                                "-fx-background-radius: 20px; " +
                                "-fx-cursor: hand;"
                )
        );
        attachButton.setOnMouseExited(e ->
                attachButton.setStyle(
                        "-fx-font-size: 18px; " +
                                "-fx-background-color: #f5f7fa; " +
                                "-fx-background-radius: 20px; " +
                                "-fx-cursor: hand;"
                )
        );
        attachButton.setOnAction(e -> sendFile());

        inputArea.getChildren().addAll(messageField, attachButton, sendButton);

        // Add a file transfer progress label
        Label fileTransferLabel = new Label("");
        fileTransferLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");

        inputContainer.getChildren().addAll(inputArea, fileTransferLabel);
        chatArea.setBottom(inputContainer);

        // Use SplitPane to divide screen into two resizable parts
        splitPane = new SplitPane();
        splitPane.getItems().addAll(contactsScroll, chatArea);
        splitPane.setDividerPositions(0.3);
        splitPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Set the SplitPane as the root of the scene
        root.setCenter(splitPane);

        // Show empty state message in chat area
        showEmptyStateIfNeeded();

        Scene scene = new Scene(root, 1000, 650); // Larger default size
        primaryStage.setTitle("SecureYAC - " + username + " (Port: " + port + ")");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        primaryStage.show();

        // Set up close event handler
        primaryStage.setOnCloseRequest(e -> {
            executorService.shutdown();
            System.exit(0);
        });
    }

    // Method to filter contacts based on search text
    private void filterContacts(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            // Show all contacts
            contactsList.getChildren().clear();
            for (String contactName : contactBoxes.keySet()) {
                contactsList.getChildren().add(contactBoxes.get(contactName));
            }
        } else {
            // Filter contacts by name
            contactsList.getChildren().clear();
            String searchLower = searchText.toLowerCase();

            for (String contactName : contactBoxes.keySet()) {
                if (contactName.toLowerCase().contains(searchLower)) {
                    contactsList.getChildren().add(contactBoxes.get(contactName));
                }
            }
        }
    }

    private void showEmptyStateIfNeeded() {
        // Check if there are no contacts
        if (contactsList.getChildren().isEmpty()) {
            // Clear chat box
            chatBox.getChildren().clear();

            // Create empty state container
            VBox emptyStateContainer = new VBox(20);
            emptyStateContainer.setAlignment(Pos.CENTER);
            emptyStateContainer.setPadding(new Insets(50));
            emptyStateContainer.setMaxWidth(Double.MAX_VALUE);
            emptyStateContainer.setMaxHeight(Double.MAX_VALUE);

            // Create a better waving hand with animation and background
            StackPane handContainer = new StackPane();
            handContainer.setAlignment(Pos.CENTER);

            // Colorful circular background for the emoji
            Circle emojiBackground = new Circle(50);
            emojiBackground.setFill(Color.web("#3498db", 0.15));
            emojiBackground.setStroke(Color.web("#3498db", 0.3));
            emojiBackground.setStrokeWidth(2);

            Label handEmoji = new Label("👋");
            handEmoji.setStyle("-fx-font-size: 80px;");

            // Add subtle waving animation
            RotateTransition rotate = new RotateTransition(Duration.millis(1000), handEmoji);
            rotate.setFromAngle(-15);
            rotate.setToAngle(15);
            rotate.setCycleCount(Animation.INDEFINITE);
            rotate.setAutoReverse(true);
            rotate.setInterpolator(Interpolator.EASE_BOTH);
            rotate.play();

            handContainer.getChildren().addAll(emojiBackground, handEmoji);

            // Add welcome message
            Label welcomeLabel = new Label("Welcome to SecureYAC!");
            welcomeLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

            // Add instructions
            Label instructionsLabel = new Label("Your chat is ready to go. Click the + button in\nthe contacts list to add your first contact.");
            instructionsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #555555; -fx-text-alignment: center;");
            instructionsLabel.setAlignment(Pos.CENTER);
            instructionsLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            // Add action button
            Button addContactButton = new Button("Add Your First Contact");
            addContactButton.setStyle(
                    "-fx-background-color: linear-gradient(to right, #3498db, #2980b9); " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 20px; " +
                            "-fx-padding: 12 30; " +
                            "-fx-font-size: 14px; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 1);"
            );

            // Hover effect
            addContactButton.setOnMouseEntered(e ->
                    addContactButton.setStyle(
                            "-fx-background-color: linear-gradient(to right, #2980b9, #3498db); " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-background-radius: 20px; " +
                                    "-fx-padding: 12 30; " +
                                    "-fx-font-size: 14px; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 1);"
                    )
            );

            addContactButton.setOnMouseExited(e ->
                    addContactButton.setStyle(
                            "-fx-background-color: linear-gradient(to right, #3498db, #2980b9); " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-background-radius: 20px; " +
                                    "-fx-padding: 12 30; " +
                                    "-fx-font-size: 14px; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 1);"
                    )
            );

            // Set button action to show add contact dialog
            addContactButton.setOnAction(e -> showAddContact());

            // Add all elements to container with some spacing
            emptyStateContainer.getChildren().addAll(
                    handContainer,
                    welcomeLabel,
                    instructionsLabel,
                    new Region() {{ setMinHeight(10); }},
                    addContactButton
            );

            // Add container to chat box
            chatBox.getChildren().add(emptyStateContainer);

            // Update UI state
            if (topBarUsername != null) {
                topBarUsername.setText("Welcome, " + username + "!");
            }
        }
    }

    // Method to change profile picture
    private void changeProfilePicture() {
        // Create a custom dialog
        Dialog<File> dialog = new Dialog<>();
        dialog.setTitle("Change Profile Picture");
        dialog.setHeaderText(null);

        // Custom styling
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");
        dialogPane.setPrefWidth(400);

        // Create main container
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setAlignment(Pos.CENTER);

        // Header
        Label headerTitle = new Label("Change Your Profile Picture");
        headerTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        // Current profile image display
        ImageView currentProfileView = new ImageView();
        currentProfileView.setFitWidth(150);
        currentProfileView.setFitHeight(150);

        // Make the image circular
        Circle imageClip = new Circle(75, 75, 75);
        currentProfileView.setClip(imageClip);

        // Border for image
        StackPane imageContainer = new StackPane();
        Circle imageBorder = new Circle(76, 76, 76);
        imageBorder.setFill(Color.TRANSPARENT);
        imageBorder.setStroke(Color.web("#3498db"));
        imageBorder.setStrokeWidth(2);

        // Set current image
        if (userProfileImagePath != null) {
            currentProfileView.setImage(new Image("file:" + userProfileImagePath));
        } else {
            currentProfileView.setImage(new Image(APP_ICON_PATH));
        }

        imageContainer.getChildren().addAll(currentProfileView, imageBorder);

        // File path for the selected image
        final File[] selectedImageFile = new File[1];

        // Button to choose new image
        Button chooseButton = new Button("Choose New Picture");
        chooseButton.setStyle(
                "-fx-background-color: #3498db; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 5; " +
                        "-fx-padding: 10 20; " +
                        "-fx-cursor: hand;"
        );

        chooseButton.setOnMouseEntered(e ->
                chooseButton.setStyle(
                        "-fx-background-color: #2980b9; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-weight: bold; " +
                                "-fx-background-radius: 5; " +
                                "-fx-padding: 10 20; " +
                                "-fx-cursor: hand;"
                )
        );

        chooseButton.setOnMouseExited(e ->
                chooseButton.setStyle(
                        "-fx-background-color: #3498db; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-weight: bold; " +
                                "-fx-background-radius: 5; " +
                                "-fx-padding: 10 20; " +
                                "-fx-cursor: hand;"
                )
        );

        chooseButton.setOnAction(e -> {
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
                currentProfileView.setImage(image);
            }
        });

        mainContainer.getChildren().addAll(headerTitle, imageContainer, chooseButton);
        dialog.getDialogPane().setContent(mainContainer);

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Style the buttons
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setStyle(
                "-fx-background-color: #3498db; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold;"
        );

        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return selectedImageFile[0];
            }
            return null;
        });

        Optional<File> result = dialog.showAndWait();

        result.ifPresent(file -> {
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

                    // Also update the profile info in server messages
                    JSONObject profileUpdateMessage = new JSONObject();
                    profileUpdateMessage.put("messageType", "PROFILE_UPDATE");
                    profileUpdateMessage.put("username", username);
                    profileUpdateMessage.put("hasCustomImage", true);
                    serverThread.sendMessage(profileUpdateMessage.toString());

                    // Show confirmation
                    showInfoAlert("Profile Updated", "Profile picture updated successfully",
                            "Your new profile picture has been saved and will be visible to your contacts.");

                } catch (IOException e) {
                    showErrorAlert("Error", "Could not save profile image", e.getMessage());
                }
            }
        });
    }

    private void showInfoAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // Style the alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");

        // Style the header
        Label headerLabel = (Label) dialogPane.lookup(".header-panel .label");
        if (headerLabel != null) {
            headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        }

        // Style the content
        Label contentLabel = (Label) dialogPane.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        }

        // Style the button
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setStyle(
                "-fx-background-color: #3498db; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold;"
        );

        alert.showAndWait();
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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
            String timestamp = now.format(formatter);

            // Update contact activity time
            contactActivities.put(currentChat, new ContactActivity(currentChat, now));

            // Add the message to history with timestamp
            messageHistory.get(currentChat).add(new String[]{"You", message, timestamp});
            updateChatBox(currentChat);
            updateLastMessage(currentChat, message);
            updateLastMessageTimestamp(currentChat, timestamp);

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

            // Sort contacts by activity time
            sortContactsByActivity();
        }
    }

    private void addMessage(String sender, String text, String timestamp, boolean isUser) {
        HBox messageContainer = new HBox();
        messageContainer.setPadding(new Insets(5));
        messageContainer.setMaxWidth(Double.MAX_VALUE);

        // Format timestamp for display
        String displayTime;
        try {
            LocalDateTime msgTime = LocalDateTime.parse(timestamp,
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
            displayTime = msgTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            displayTime = "00:00";
        }

        VBox contentBox = new VBox(3);
        contentBox.setMaxWidth(450);  // Max width for messages

        if (isUser) {
            // User messages aligned to the right
            messageContainer.setAlignment(Pos.CENTER_RIGHT);
            contentBox.setAlignment(Pos.TOP_RIGHT);
        } else if ("SYSTEM".equals(sender)) {
            // System messages centered
            messageContainer.setAlignment(Pos.CENTER);
            contentBox.setAlignment(Pos.CENTER);
        } else {
            // Other users' messages aligned to the left
            messageContainer.setAlignment(Pos.CENTER_LEFT);
            contentBox.setAlignment(Pos.TOP_LEFT);
        }

        // Sender label
        if (!"SYSTEM".equals(sender)) {
            Label senderLabel = new Label(isUser ? "You" : sender);
            senderLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-font-weight: bold;");
            contentBox.getChildren().add(senderLabel);
        }

        // Message bubble
        HBox bubbleBox = new HBox();
        if (isUser) {
            bubbleBox.setAlignment(Pos.CENTER_RIGHT);
        } else if ("SYSTEM".equals(sender)) {
            bubbleBox.setAlignment(Pos.CENTER);
        } else {
            bubbleBox.setAlignment(Pos.CENTER_LEFT);
        }

        // Text content with proper wrapping
        Label messageText = new Label(text);
        messageText.setWrapText(true);
        messageText.setMaxWidth(400);

        StackPane messageBubble = new StackPane();
        messageBubble.setPadding(new Insets(12, 15, 12, 15));
        messageBubble.getChildren().add(messageText);

        if (isUser) {
            // User messages - blue gradient bubbles
            messageBubble.setStyle(
                    "-fx-background-color: linear-gradient(to right, #3498db, #2980b9); " +
                            "-fx-background-radius: 18 18 0 18; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);"
            );
            messageText.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        } else if ("SYSTEM".equals(sender)) {
            // System messages - neutral gray
            messageBubble.setStyle(
                    "-fx-background-color: #F0F2F5; " +
                            "-fx-background-radius: 18; " +
                            "-fx-border-color: #E4E6EB; " +
                            "-fx-border-radius: 18; " +
                            "-fx-border-width: 1px;"
            );
            messageText.setStyle("-fx-text-fill: #34495e; -fx-font-size: 14px;");
        } else {
            // Others' messages - light gray bubbles
            messageBubble.setStyle(
                    "-fx-background-color: #F0F2F5; " +
                            "-fx-background-radius: 18 18 18 0; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);"
            );
            messageText.setStyle("-fx-text-fill: #34495e; -fx-font-size: 14px;");
        }

        bubbleBox.getChildren().add(messageBubble);

        // Add timestamp label below the message
        Label timestampLabel = new Label(displayTime);
        timestampLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 10px;");

        // Add bubble and timestamp to content box
        contentBox.getChildren().addAll(bubbleBox, timestampLabel);

        messageContainer.getChildren().add(contentBox);

        if (!isUser && !"SYSTEM".equals(sender)) {
            // Add spacer to properly align messages
            Region spacer = new Region();
            spacer.setMinWidth(10);
            messageContainer.getChildren().addFirst(spacer);
        }

        chatBox.getChildren().add(messageContainer);
    }

    private void addContact(String name, String lastMessage, String imageUrl) {
        HBox contactBox = new HBox(15);
        contactBox.setPadding(new Insets(12));
        contactBox.setAlignment(Pos.CENTER_LEFT);
        contactBox.setPrefHeight(70);
        contactBox.setMinHeight(70);
        contactBox.setMaxHeight(70);
        contactBox.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 1);"
        );

        // Contact image with circular clip
        ImageView contactImage = new ImageView(new Image(imageUrl));
        contactImage.setFitWidth(45);
        contactImage.setFitHeight(45);

        // Make contact image circular
        Circle contactClip = new Circle(22.5, 22.5, 22.5);
        contactImage.setClip(contactClip);

        // Left section with image
        StackPane imageContainer = new StackPane(contactImage);
        imageContainer.setMinWidth(45);

        // Middle section with name and message
        VBox textContainer = new VBox(3);
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #34495e;");

        // Create last message text
        Label lastMessageLabel = new Label(lastMessage);
        lastMessageLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 12px;");
        lastMessageLabel.setMaxWidth(200);
        lastMessageLabel.setMaxHeight(20);
        lastMessageLabel.setEllipsisString("...");
        lastMessageLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

        textContainer.getChildren().addAll(nameLabel, lastMessageLabel);

        // Ensure the text container doesn't grow too tall
        textContainer.setMaxHeight(50);

        // Right section with time and notification
        VBox rightContainer = new VBox(5);
        rightContainer.setAlignment(Pos.TOP_RIGHT);
        rightContainer.setMinWidth(60);
        rightContainer.setPrefWidth(60);

        // Time label
        Label timeLabel = new Label("");
        timeLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
        timeLabel.setAlignment(Pos.CENTER_RIGHT);
        timeLabel.setMaxWidth(Double.MAX_VALUE); // Allow the label to take full width of parent

        // Create unread indicator
        StackPane unreadIndicator = new StackPane();
        unreadIndicator.setAlignment(Pos.CENTER_RIGHT);
        unreadIndicator.setMaxWidth(Double.MAX_VALUE);
        unreadIndicator.setVisible(false);

        rightContainer.getChildren().addAll(timeLabel, unreadIndicator);

        // Add the three sections to the contact box
        contactBox.getChildren().addAll(imageContainer, textContainer, rightContainer);

        // Store references for easy updates
        contactBoxes.put(name, contactBox);
        lastMessageLabels.put(name, lastMessageLabel);
        unreadIndicators.put(name, unreadIndicator);

        // Define styles for different states
        String normalStyle = "-fx-background-color: white; " +
                "-fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 1);";

        String hoverStyle = "-fx-background-color: #f5f7fa; " +
                "-fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 1);";

        String selectedStyle = "-fx-background-color: #EBF5FB; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: #3498db; " +
                "-fx-border-width: 0 0 0 3; " +
                "-fx-border-radius: 10 0 0 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 1);";

        // Add hover effect only when not selected
        contactBox.setOnMouseEntered(event -> {
            if (selectedContactBox != contactBox) {
                contactBox.setStyle(hoverStyle);
            }
        });

        contactBox.setOnMouseExited(event -> {
            if (selectedContactBox != contactBox) {
                contactBox.setStyle(normalStyle);
            }
        });

        // Initialize message history if needed
        if (!messageHistory.containsKey(name)) {
            messageHistory.put(name, new ArrayList<>());
        }

        // Add the initial message if there's message history
        if (messageHistory.get(name).isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
            String fullTimestamp = now.format(fullFormatter);

            // Format the display time
            String displayTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));
            timeLabel.setText(displayTime);

            messageHistory.get(name).add(new String[]{name, lastMessage, fullTimestamp});

            // Initialize contact activity time
            contactActivities.put(name, new ContactActivity(name, now));
        } else {
            // Get the most recent message timestamp and update the time label
            String[] lastMsg = messageHistory.get(name).getLast();
            try {
                LocalDateTime msgTime = LocalDateTime.parse(lastMsg[2],
                        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                timeLabel.setText(msgTime.format(DateTimeFormatter.ofPattern("HH:mm")));

                // Initialize contact activity time from existing message
                contactActivities.put(name, new ContactActivity(name, msgTime));
            } catch (Exception e) {
                timeLabel.setText("--:--");
                // Initialize with current time if timestamp can't be parsed
                contactActivities.put(name, new ContactActivity(name, LocalDateTime.now()));
            }
        }

        // Initialize unread messages counter
        if (!unreadMessages.containsKey(name)) {
            unreadMessages.put(name, 0);
        }

        contactBox.setOnMouseClicked(event -> {
            // Reset unread counter when chat is opened
            unreadMessages.put(name, 0);
            updateUnreadIndicator(name);

            currentChat = name;
            updateChatBox(name);
            updateTopBar(name, imageUrl);

            // Reset style for previously selected contact
            if (selectedContactBox != null) {
                selectedContactBox.setStyle(normalStyle);
            }

            // Apply selected style to current contact
            contactBox.setStyle(selectedStyle);

            // Update the currently selected contact box
            selectedContactBox = contactBox;
        });

        if (contactsList.getChildren().size() == 1 &&
                chatBox.getChildren().size() == 1 &&
                chatBox.getChildren().getFirst() instanceof VBox) {
            // Clear the empty state and update the top bar
            chatBox.getChildren().clear();
            topBarUsername.setText("Select a Contact");
        }

        // If this contact is the current chat, apply selected style
        if (name.equals(currentChat)) {
            contactBox.setStyle(selectedStyle);
            selectedContactBox = contactBox;
        }

        // Add to the contacts list (will be sorted later)
        contactsList.getChildren().add(contactBox);

        // Sort contacts after adding a new one
        sortContactsByActivity();
    }

    private void updateTopBar(String name, String imageUrl) {
        topBarUsername.setText(name);
        topBarProfileImage.setImage(new Image(imageUrl));
    }

    private void updateLastMessage(String name, String newMessage) {
        if (lastMessageLabels.containsKey(name)) {
            Label messageLabel = lastMessageLabels.get(name);
            messageLabel.setText(newMessage);
        }
    }

    private void updateLastMessageTimestamp(String name, String timestamp) {
        HBox contactBox = contactBoxes.get(name);
        if (contactBox != null && contactBox.getChildren().size() >= 3) {
            VBox rightContainer = (VBox) contactBox.getChildren().get(2);
            if (!rightContainer.getChildren().isEmpty()) {
                Label timeLabel = (Label) rightContainer.getChildren().getFirst();

                try {
                    LocalDateTime msgTime = LocalDateTime.parse(timestamp,
                            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                    timeLabel.setText(msgTime.format(DateTimeFormatter.ofPattern("HH:mm")));

                    // Update contact activity time
                    ContactActivity activity = contactActivities.get(name);
                    if (activity != null) {
                        activity.setLastActivity(msgTime);
                    } else {
                        contactActivities.put(name, new ContactActivity(name, msgTime));
                    }
                } catch (Exception e) {
                    timeLabel.setText("--:--");
                }
            }
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

    private void sortContactsByActivity() {
        // Create a list of contacts sorted by activity time
        List<ContactActivity> sortedContacts = new ArrayList<>(contactActivities.values());
        java.util.Collections.sort(sortedContacts);

        // Clear and re-add contacts in sorted order
        contactsList.getChildren().clear();

        for (ContactActivity activity : sortedContacts) {
            String contactName = activity.getContactName();
            if (contactBoxes.containsKey(contactName)) {
                contactsList.getChildren().add(contactBoxes.get(contactName));
            }
        }
    }

    private void showAddContact() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add Contact");
        dialog.setHeaderText(null);

        // Custom styling for the dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");
        dialogPane.setPrefWidth(400);

        // Create main container
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));

        // Header with icon
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label contactIcon = new Label("👤");
        contactIcon.setStyle("-fx-font-size: 30px;");

        VBox headerTextBox = new VBox(5);
        Label headerTitle = new Label("Add a New Contact");
        headerTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        Label headerSubtitle = new Label("Connect to another SecureYAC user");
        headerSubtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        headerTextBox.getChildren().addAll(headerTitle, headerSubtitle);
        headerBox.getChildren().addAll(contactIcon, headerTextBox);

        // Input field section
        VBox inputBox = new VBox(10);
        inputBox.setStyle(
                "-fx-background-color: #f5f7fa; " +
                        "-fx-background-radius: 10; " +
                        "-fx-padding: 15;"
        );

        Label addressLabel = new Label("Contact Address");
        addressLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e;");

        Label addressInstructions = new Label("Enter in format: hostname:port");
        addressInstructions.setStyle("-fx-text-fill: #7f8c8d;");

        TextField addressField = new TextField();
        addressField.setPromptText("e.g., localhost:8000");
        addressField.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 5; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-radius: 5; " +
                        "-fx-padding: 8;"
        );

        inputBox.getChildren().addAll(addressLabel, addressInstructions, addressField);

        // Examples section
        VBox examplesBox = new VBox(10);
        examplesBox.setStyle("-fx-padding: 5 15;");

        Label examplesLabel = new Label("Examples");
        examplesLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #7f8c8d;");

        Label localExample = new Label("• Local connection: localhost:8000");
        localExample.setStyle("-fx-text-fill: #95a5a6;");

        Label remoteExample = new Label("• Remote connection: 192.168.1.5:8000");
        remoteExample.setStyle("-fx-text-fill: #95a5a6;");

        examplesBox.getChildren().addAll(examplesLabel, localExample, remoteExample);

        mainContainer.getChildren().addAll(headerBox, inputBox, examplesBox);
        dialog.getDialogPane().setContent(mainContainer);

        // Set the button types
        ButtonType addButtonType = new ButtonType("Add Contact", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, cancelButtonType);

        // Style the buttons
        Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setStyle(
                "-fx-background-color: #3498db; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold;"
        );

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
                        } else {
                            showErrorAlert("Already Connected", "Contact already exists",
                                    "You are already connected to " + contactId);
                        }
                    }
                } else {
                    showErrorAlert("Invalid Format", "Invalid address format",
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

        // Style the alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");

        // Style the header
        Label headerLabel = (Label) dialogPane.lookup(".header-panel .label");
        if (headerLabel != null) {
            headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        }

        // Style the content
        Label contentLabel = (Label) dialogPane.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        }

        // Style the button
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setStyle(
                "-fx-background-color: #e74c3c; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold;"
        );

        alert.showAndWait();
    }

    private static class ContactActivity implements Comparable<ContactActivity> {
        private String contactName;
        private LocalDateTime lastActivity;

        public ContactActivity(String contactName, LocalDateTime lastActivity) {
            this.contactName = contactName;
            this.lastActivity = lastActivity;
        }

        public String getContactName() {
            return contactName;
        }

        public LocalDateTime getLastActivity() {
            return lastActivity;
        }

        public void setLastActivity(LocalDateTime lastActivity) {
            this.lastActivity = lastActivity;
        }

        @Override
        public int compareTo(ContactActivity other) {
            // Sort in reverse chronological order (most recent first)
            return other.lastActivity.compareTo(this.lastActivity);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
