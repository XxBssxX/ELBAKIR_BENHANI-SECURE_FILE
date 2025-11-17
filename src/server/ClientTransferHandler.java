package server;

import crypto.CryptoUtils;
import crypto.KeyManager;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClientTransferHandler extends Thread {
    
    private Socket clientSocket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private SecretKey secretKey;
    
    public ClientTransferHandler(Socket socket) throws Exception {
        this.clientSocket = socket;
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
        this.secretKey = KeyManager.loadKey();
    }
    
    @Override
    public void run() {
        try {
            System.out.println("Nouveau client connecté: " + clientSocket.getRemoteSocketAddress());
            
            
            if (!handleAuthentication()) {
                System.out.println("Authentification échouée pour: " + clientSocket.getRemoteSocketAddress());
                return;
            }
            
            
            FileMetadata metadata = handleNegotiation();
            if (metadata == null) {
                System.out.println("Négociation échouée pour: " + clientSocket.getRemoteSocketAddress());
                return;
            }
            
           
            handleTransfer(metadata);
            
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                System.out.println("Connexion fermée avec: " + clientSocket.getRemoteSocketAddress());
            } catch (IOException e) {
                System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
            }
        }
    }
    
    private boolean handleAuthentication() throws IOException {
        
        int loginLength = dis.readInt();
        byte[] loginBytes = new byte[loginLength];
        dis.readFully(loginBytes);
        String login = new String(loginBytes);
        
      
        int passwordLength = dis.readInt();
        byte[] passwordBytes = new byte[passwordLength];
        dis.readFully(passwordBytes);
        String password = new String(passwordBytes);
        
        
        boolean isValid = UserCredentials.validate(login, password);
        
        if (isValid) {
            dos.writeUTF("AUTH_OK");
            dos.flush();
            System.out.println("Authentification réussie pour: " + login);
            return true;
        } else {
            dos.writeUTF("AUTH_FAIL");
            dos.flush();
            System.out.println("Authentification échouée pour: " + login);
            return false;
        }
    }
    
    private FileMetadata handleNegotiation() throws IOException {
        
        int nameLength = dis.readInt();
        byte[] nameBytes = new byte[nameLength];
        dis.readFully(nameBytes);
        String fileName = new String(nameBytes);
        
        
        long fileSize = dis.readLong();
        
        
        int hashLength = dis.readInt();
        byte[] hashBytes = new byte[hashLength];
        dis.readFully(hashBytes);
        String expectedHash = new String(hashBytes);
        
        FileMetadata metadata = new FileMetadata(fileName, fileSize, expectedHash);
        
        dos.writeUTF("READY_FOR_TRANSFER");
        dos.flush();
        
        System.out.println("Négociation réussie - Fichier: " + fileName + ", Taille: " + fileSize + " bytes");
        
        return metadata;
    }
    
    private void handleTransfer(FileMetadata metadata) throws Exception {
       
        int encryptedDataLength = dis.readInt();
        
        
        byte[] encryptedData = new byte[encryptedDataLength];
        dis.readFully(encryptedData);
        
        
        byte[] decryptedData = CryptoUtils.decrypt(encryptedData, secretKey);
        
        
        String savePath = "received_" + metadata.fileName;
        Files.write(Paths.get(savePath), decryptedData);
        
        
        String calculatedHash = CryptoUtils.calculateSHA256(decryptedData);
        
        if (calculatedHash.equalsIgnoreCase(metadata.hash)) {
            dos.writeUTF("TRANSFER_SUCCESS");
            dos.flush();
            System.out.println("Transfert réussi - Fichier sauvegardé: " + savePath);
        } else {
            dos.writeUTF("TRANSFER_FAIL");
            dos.flush();
            System.err.println("Échec de vérification d'intégrité!");
            System.err.println("Hachage attendu: " + metadata.hash);
            System.err.println("Hachage calculé: " + calculatedHash);
        }
    }
    private static class FileMetadata {
        String fileName;
        long fileSize;
        String hash;
        
        FileMetadata(String fileName, long fileSize, String hash) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.hash = hash;
        }
    }
}


