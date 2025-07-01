# School of Computing &mdash; Year 4 Project

## SecureYAC
|            |                              |
|------------|------------------------------|
|Supervisor: | Geoff Hamilton               |
|Student:    | Eryk Zygmunt Styczynski      |
|Student:    | Liucija Paulina Adomaviciute |
---------------------------------------------

[![Gradle Build and Test](https://github.com/liucijaad/secureYAC/actions/workflows/gradle.yml/badge.svg)](https://github.com/liucijaad/secureYAC/actions/workflows/gradle.yml)

SecureYAC is a messaging application with a focus on encryption and security. The goal of the project is to provide a cryptographically secure communications channel that does not depend on third-parties.  It establishes a direct connection between users using a custom peer-to-peer networking protocol based on Tox. The identity creation, verification, and authentication is handled by the X3DH key agreement protocol. The users export key bundles and share them with others to establish connection and the Double Ratchet algorithm is used to exchange encrypted messages.

**Project Areas:**
* Cryptography
* Security
* Instant Messaging

**Technologies Used:**

* Java
* JUnit
* Gradle
* BouncyCastle Cryptographic Library

**Personal Contribution**

For this project, I implemented Elliptic Curve Cryptography (ECC) without using any cryptographic libraries for the main encryption/decryption algorithms. I implemented X25519 Elliptic Curve key generation algorithm, Extended Triple Diffie-Hellman (X3DH) and Double Ratchet algorithms based on [Signal's specification](https://signal.org/docs/). I implemented XEdDSA signature scheme. BouncyCastle cyrptographic library was only used for some hashing algorithms that I did not implement from scratch due to time contraints. All unit tests were written by me. For more detailed information and specification, refer to [technical manual](./docs/technical_manual.pdf).

