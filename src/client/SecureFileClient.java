package client;

import crypto.CryptoUtils;
import crypto.KeyManager;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class SecureFileClient {
    
    private String serverIP;
    private int serverPort;
    private String login;
    private String password;
    private String filePath;
    private SecretKey secretKey;
    
    public SecureFileClient(String serverIP, int serverPort, String login, String password, String filePath) throws Exception {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.login = login;
        this.password = password;
        this.filePath = filePath;
        this.secretKey = KeyManager.loadKey();
    }
    
    public void transferFile() {
        try (Socket socket = new Socket(serverIP, serverPort);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
            
            System.out.println("Connecté au serveur: " + serverIP + ":" + serverPort);
            
          
            if (!performAuthentication(dos, dis)) {
                System.err.println("Échec de l'authentification!");
                return;
            }
            System.out.println("Authentification réussie!");
            
            
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                System.err.println("Le fichier n'existe pas: " + filePath);
                return;
            }
            
            String hash = CryptoUtils.calculateSHA256(file);
            if (!performNegotiation(dos, dis, file, hash)) {
                System.err.println("Échec de la négociation!");
                return;
            }
            System.out.println("Négociation réussie!");
            
            
            if (!performTransfer(dos, dis, file)) {
                System.err.println("Échec du transfert!");
                return;
            }
            System.out.println("Transfert réussi!");
            
        } catch (IOException e) {
            System.err.println("Erreur de communication: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private boolean performAuthentication(DataOutputStream dos, DataInputStream dis) throws IOException {
        
        byte[] loginBytes = login.getBytes();
        dos.writeInt(loginBytes.length);
        dos.write(loginBytes);
        
        
        byte[] passwordBytes = password.getBytes();
        dos.writeInt(passwordBytes.length);
        dos.write(passwordBytes);
        dos.flush();
        
        
        String response = dis.readUTF();
        return "AUTH_OK".equals(response);
    }
    private boolean performNegotiation(DataOutputStream dos, DataInputStream dis, File file, String hash) throws IOException {
        
        String fileName = file.getName();
        byte[] nameBytes = fileName.getBytes();
        dos.writeInt(nameBytes.length);
        dos.write(nameBytes);
        
        dos.writeLong(file.length());
        
       
        byte[] hashBytes = hash.getBytes();
        dos.writeInt(hashBytes.length);
        dos.write(hashBytes);
        dos.flush();
        
        String response = dis.readUTF();
        return "READY_FOR_TRANSFER".equals(response);
    }
    private boolean performTransfer(DataOutputStream dos, DataInputStream dis, File file) throws Exception {
        
        byte[] fileData = Files.readAllBytes(Paths.get(file.getPath()));
        
        System.out.println("Chiffrement du fichier...");
        byte[] encryptedData = CryptoUtils.encrypt(fileData, secretKey);
        
       
        dos.writeInt(encryptedData.length);
        
       
        System.out.println("Envoi du fichier chiffré (" + encryptedData.length + " bytes)...");
        dos.write(encryptedData);
        dos.flush();
        
        String response = dis.readUTF();
        return "TRANSFER_SUCCESS".equals(response);
    }
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Client de Transfert de Fichiers Sécurisé ===\n");
        
       
        System.out.print("Adresse IP du serveur (ex: 192.168.1.100): ");
        String serverIP = scanner.nextLine().trim();
        if (serverIP.isEmpty()) {
            serverIP = "localhost";
        }
        
       
        System.out.print("Port du serveur (défaut: 8888): ");
        String portStr = scanner.nextLine().trim();
        int serverPort = 8888;
        if (!portStr.isEmpty()) {
            try {
                serverPort = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.out.println("Port invalide, utilisation du port par défaut: 8888");
            }
        }

        System.out.print("Login: ");
        String login = scanner.nextLine().trim();
        
        
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        
        
        System.out.print("Chemin du fichier à transférer: ");
        String filePath = scanner.nextLine().trim();
        
        scanner.close();
        
        try {
            SecureFileClient client = new SecureFileClient(serverIP, serverPort, login, password, filePath);
            client.transferFile();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du client: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


