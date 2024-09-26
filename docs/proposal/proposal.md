# School of Computing &mdash; Year 4 Project Proposal Form

## SECTION A

|                     |                              |
|---------------------|------------------------------|
|Project Title:       | Secure Chat                  |
|Student 1 Name:      | Liucija Paulina Adomaviciute |
|Student 1 ID:        | 21790411                     |
|Student 2 Name:      | Eryk Zygmunt Styczynski      |
|Student 2 ID:        | 21753851                     |
|Project Supervisor:  | Geoffrey Hamilton            |


## SECTION B


### Introduction

The primary objective of this project is to offer an open-source, non-profit secure messaging application. With this application, users will be able to set up private encrypted peer-to-peer connections for secure communication. As a minimum, the application will support LAN-based communication for testing purposes, but the focus will be on building a distributed P2P network for messaging, image sharing, and file transfers.

### Outline

Our proposed project aims to develop a secure messaging application that allows users to communicate privately via peer-to-peer connections, with no external server involved. This will offer a distributed architecture, allowing users to initiate private chats and exchange encrypted messages, images, and files. Our application will ensure confidentiality by encrypting and decrypting data on both ends.

### Background

The inspiration for this project came from our general interest in cybersecurity, cryptography, and privacy. With huge data leaks becoming more common and public, increasing data privacy concerns and the mistrust in the corporations that offer the mainstream messaging applications, there is a need for a simple, lightweight, open-source messaging application. With this project we aim to address the need for enhanced privacy and security in communication.

### Achievements

**Target Audience**

The target audience of this project is privacy and security conscious people.

**Functionality of the Application**

With this application, the user will be able to intiate private chats, send encrypted messages, images and files.

### Justification

This messaging application is not tied to any corporation and the data of the user is stored only on their local machine. The only data that is shared is the one that the user chooses to share. While there already exist some applications that offer secure encrypted messaging and file sharing features, the data usually goes through a company controlled server, where it could be stolen, stored and sold or decrypted. By eliminating the server as middle-man, we are offering additional security for peer-to-peer messaging and file sharing.

### Programming language(s)

**Java:** Primary language for backend logic, message encryption and communication protocols.

**JavaFX:** For building the frontend user interface of the application.

### Programming tools / Tech stack

**Frontend**:

-   **JavaFX**: For creating the graphical user interface.

**Backend**:

-   **Java**: For handling the application logic, encryption, and message transfers.
-   **Java Sockets**: For establishing peer-to-peer connections over LAN to send and receive messages and files.

**Encryption**:

-   **Java Cryptography Extension (JCE)**: For implementing encryption and decryption algorithms to secure the communication.

**Version Control**:

-   **Git and GitLab**: For version control and collaboration.

**Testing**:

-   **JUnit**: For unit testing of backend components.

### Hardware

No special hardware/software requirements.

### Learning Challenges

* Implementation of encryption and decryption algorithms.
* Creating a user-friendly interface.
* Designing and implementing a scalable P2P architecture.
* Configuring the application to securely send messages, images and files through P2P network.

### Breakdown of work

#### Liucija Paulina Adomaviciute

* Implementation of encryption and decryption algorithms
* Implementation of compression algorithm
* Testing
* CI/CD pipelines

#### Eryk Zygmunt Styczynski

> *Student 2 should complete this section.*
