package com.securefiletransfer;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.crypto.SecretKey;

/**
 * Thread gérant la communication avec un client spécifique
 * Implémente le protocole en 3 phases : Authentification, Négociation, Transfert
 */
public class ClientTransferHandler extends Thread {
    
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private SecretKey secretKey;
    
    public ClientTransferHandler(Socket socket) {
        this.clientSocket = socket;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.dataIn = new DataInputStream(socket.getInputStream());
            this.dataOut = new DataOutputStream(socket.getOutputStream());
            this.secretKey = CryptoUtils.getDefaultKey();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'initialisation du handler: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation de la clé: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        try {
            System.out.println("Client connecté: " + clientSocket.getRemoteSocketAddress());
            
            // Phase 1: Authentification
            if (!handleAuthentication()) {
                System.out.println("Authentification échouée pour " + clientSocket.getRemoteSocketAddress());
                return;
            }
            
            // Phase 2: Négociation
            FileMetadata metadata = handleNegotiation();
            if (metadata == null) {
                System.out.println("Négociation échouée pour " + clientSocket.getRemoteSocketAddress());
                return;
            }
            
            // Phase 3: Transfert et Vérification
            handleTransfer(metadata);
            
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }
    
    /**
     * Phase 1: Authentification
     * Reçoit login/password et vérifie les identifiants
     */
    private boolean handleAuthentication() throws IOException {
        // Recevoir login
        String login = in.readLine();
        if (login == null) {
            out.println("AUTH_FAIL");
            return false;
        }
        
        // Recevoir password
        String password = in.readLine();
        if (password == null) {
            out.println("AUTH_FAIL");
            return false;
        }
        
        // Vérifier les identifiants (stockés en dur pour la démo)
        if (Authenticator.authenticate(login, password)) {
            out.println("AUTH_OK");
            System.out.println("Authentification réussie pour: " + login);
            return true;
        } else {
            out.println("AUTH_FAIL");
            return false;
        }
    }
    
    /**
     * Phase 2: Négociation
     * Reçoit les métadonnées du fichier (nom, taille, hachage)
     */
    private FileMetadata handleNegotiation() throws IOException {
        // Recevoir le nom du fichier
        String fileName = in.readLine();
        if (fileName == null) {
            return null;
        }
        
        // Recevoir la taille
        String sizeStr = in.readLine();
        if (sizeStr == null) {
            return null;
        }
        long fileSize = Long.parseLong(sizeStr);
        
        // Recevoir le hachage SHA-256
        String hash = in.readLine();
        if (hash == null) {
            return null;
        }
        
        FileMetadata metadata = new FileMetadata(fileName, fileSize, hash);
        out.println("READY_FOR_TRANSFER");
        System.out.println("Négociation réussie - Fichier: " + fileName + ", Taille: " + fileSize + " bytes");
        
        return metadata;
    }
    
    /**
     * Phase 3: Transfert et Vérification
     * Reçoit le fichier chiffré, le déchiffre, l'enregistre et vérifie l'intégrité
     */
    private void handleTransfer(FileMetadata metadata) throws Exception {
        // Créer le dossier de réception s'il n'existe pas
        Path uploadDir = Paths.get("uploads");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        // Recevoir la taille des données chiffrées
        int encryptedSize = dataIn.readInt();
        
        // Recevoir les données chiffrées
        byte[] encryptedData = new byte[encryptedSize];
        int totalRead = 0;
        while (totalRead < encryptedSize) {
            int bytesRead = dataIn.read(encryptedData, totalRead, encryptedSize - totalRead);
            if (bytesRead == -1) {
                out.println("TRANSFER_FAIL");
                return;
            }
            totalRead += bytesRead;
        }
        
        System.out.println("Données chiffrées reçues: " + encryptedSize + " bytes");
        
        // Déchiffrer les données
        byte[] decryptedData = CryptoUtils.decrypt(encryptedData, secretKey);
        System.out.println("Données déchiffrées: " + decryptedData.length + " bytes");
        
        // Vérifier l'intégrité avec le hachage SHA-256
        String calculatedHash = CryptoUtils.calculateSHA256(decryptedData);
        if (!calculatedHash.equals(metadata.getHash())) {
            System.out.println("Échec de vérification d'intégrité!");
            System.out.println("Hachage attendu: " + metadata.getHash());
            System.out.println("Hachage calculé: " + calculatedHash);
            out.println("TRANSFER_FAIL");
            return;
        }
        
        // Enregistrer le fichier
        Path filePath = uploadDir.resolve(metadata.getFileName());
        Files.write(filePath, decryptedData);
        
        System.out.println("Fichier enregistré avec succès: " + filePath);
        out.println("TRANSFER_SUCCESS");
    }
    
    /**
     * Ferme proprement la connexion
     */
    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (dataIn != null) dataIn.close();
            if (dataOut != null) dataOut.close();
            if (clientSocket != null) clientSocket.close();
            System.out.println("Connexion fermée avec " + clientSocket.getRemoteSocketAddress());
        } catch (IOException e) {
            System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
        }
    }
    
    /**
     * Classe interne pour stocker les métadonnées du fichier
     */
    private static class FileMetadata {
        private String fileName;
        private long fileSize;
        private String hash;
        
        public FileMetadata(String fileName, long fileSize, String hash) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.hash = hash;
        }
        
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public String getHash() { return hash; }
    }
}

