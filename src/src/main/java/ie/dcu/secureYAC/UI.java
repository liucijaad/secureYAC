package ie.dcu.secureYAC;

import javafx.application.Application;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    @Override
    public void start(Stage primaryStage) {
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
        contactsList.getChildren().add(addContactButton);

        contactsScroll = new ScrollPane(contactsList);
        contactsScroll.setFitToWidth(true);
        contactsScroll.setFitToHeight(true);

        // Sample Contacts
        addContact("Eryk", "Hey, are you there?", "https://cdn2.psychologytoday.com/assets/styles/manual_crop_4_3_1200x900/public/field_blog_entry_images/2018-09/shutterstock_648907024.jpg?itok=eaVcXTz5");
        addContact("Liucija", "See you later!", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a0/Andrzej_Person_Kancelaria_Senatu.jpg/1200px-Andrzej_Person_Kancelaria_Senatu.jpg");
        addContact("Geoff", "Good morning!", "https://media.hswstatic.com/eyJidWNrZXQiOiJjb250ZW50Lmhzd3N0YXRpYy5jb20iLCJrZXkiOiJnaWZcL3BsYXlcLzBiN2Y0ZTliLWY1OWMtNDAyNC05ZjA2LWIzZGMxMjg1MGFiNy0xOTIwLTEwODAuanBnIiwiZWRpdHMiOnsicmVzaXplIjp7IndpZHRoIjo4Mjh9fX0=");

        // Chat area
        BorderPane chatArea = new BorderPane();

        // Top bar with the person we are chatting with
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button menuButton = new Button("☰");
        menuButton.setStyle("-fx-font-size: 18px; -fx-background-color: transparent;");
        menuButton.setOnAction(e -> toggleContacts());

        topBarProfileImage = new ImageView(new Image("https://img.freepik.com/free-photo/lifestyle-people-emotions-casual-concept-confident-nice-smiling-asian-woman-cross-arms-chest-confident-ready-help-listening-coworkers-taking-part-conversation_1258-59335.jpg"));
        topBarProfileImage.setFitWidth(50);
        topBarProfileImage.setFitHeight(50);
        topBarUsername = new Label("Select a Contact");
        topBarUsername.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        topBar.getChildren().addAll(menuButton, topBarProfileImage, topBarUsername);
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
        Button sendButton = new Button("Send");

        sendButton.setOnAction(e -> {
            if (currentChat != null) {
                String message = messageField.getText().trim();
                if (!message.isEmpty()) {
                    messageHistory.get(currentChat).add(new String[]{"You", message});
                    updateChatBox(currentChat);
                    updateLastMessage(currentChat, message);
                    messageField.clear();
                }
            }
        });

        inputArea.getChildren().addAll(messageField, sendButton);
        chatArea.setBottom(inputArea);

        // Use SplitPane to divide screen into two resizable parts
        splitPane = new SplitPane();
        splitPane.getItems().addAll(contactsScroll, chatArea);
        splitPane.setDividerPositions(0.3);

        // Set the SplitPane as the root of the scene
        root.setCenter(splitPane);

        Scene scene = new Scene(root, 800, 500);
        primaryStage.setTitle("SecureYAC");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void addMessage(String sender, String text, boolean isUser) {
        VBox messageContainer = new VBox(2);
        messageContainer.setPadding(new Insets(5));

        Label senderLabel = new Label(isUser ? "You" : sender);
        senderLabel.setStyle("-fx-text-fill: grey; -fx-font-size: 12px;");

        Text messageText = new Text(text);
        messageText.setStyle("-fx-font-size: 14px;");

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

        messageContainer.getChildren().addAll(senderLabel, messageBubble);
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

        messageHistory.put(name, new ArrayList<>());
        messageHistory.get(name).add(new String[]{name, lastMessage});

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
            addMessage(message[0], message[1], message[0].equals("You"));
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
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Contact");
        dialog.setHeaderText("Add a new contact");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty() && !messageHistory.containsKey(name)) {
                addContact(name, "New contact", "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png");
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}