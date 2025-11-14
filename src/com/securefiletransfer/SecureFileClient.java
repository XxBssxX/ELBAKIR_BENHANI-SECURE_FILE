package com.securefiletransfer;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.crypto.SecretKey;

/**
 * Client pour le transfert de fichiers sécurisé
 * Interface en ligne de commande pour envoyer un fichier au serveur
 */
public class SecureFileClient {
    
    private String serverAddress;
    private int serverPort;
    private String login;
    private String password;
    private String filePath;
    private SecretKey secretKey;
    
    public SecureFileClient(String serverAddress, int serverPort, String login, 
                           String password, String filePath) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.login = login;
        this.password = password;
        this.filePath = filePath;
    }
    
    /**
     * Méthode principale pour effectuer le transfert
     */
    public boolean transferFile() {
        Socket socket = null;
        BufferedReader in = null;
        PrintWriter out = null;
        DataInputStream dataIn = null;
        DataOutputStream dataOut = null;
        
        try {
            // Initialiser la clé secrète (même clé que le serveur)
            secretKey = CryptoUtils.getDefaultKey();
            
            // Se connecter au serveur
            System.out.println("Connexion au serveur " + serverAddress + ":" + serverPort + "...");
            socket = new Socket(serverAddress, serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            dataIn = new DataInputStream(socket.getInputStream());
            dataOut = new DataOutputStream(socket.getOutputStream());
            
            System.out.println("Connecté au serveur.");
            
            // Phase 1: Authentification
            if (!performAuthentication(out, in)) {
                System.err.println("Échec de l'authentification.");
                return false;
            }
            System.out.println("Authentification réussie.");
            
            // Phase 2: Négociation
            Path file = Paths.get(filePath);
            if (!Files.exists(file)) {
                System.err.println("Le fichier n'existe pas: " + filePath);
                return false;
            }
            
            // Lire le fichier
            byte[] fileData = Files.readAllBytes(file);
            String fileName = file.getFileName().toString();
            
            // Calculer le hachage SHA-256
            String hash = CryptoUtils.calculateSHA256(fileData);
            System.out.println("Hachage SHA-256 calculé: " + hash);
            
            // Chiffrer le fichier
            System.out.println("Chiffrement du fichier...");
            byte[] encryptedData = CryptoUtils.encrypt(fileData, secretKey);
            System.out.println("Fichier chiffré: " + fileData.length + " bytes -> " + encryptedData.length + " bytes");
            
            // Envoyer les métadonnées
            if (!performNegotiation(out, in, fileName, fileData.length, hash)) {
                System.err.println("Échec de la négociation.");
                return false;
            }
            System.out.println("Négociation réussie.");
            
            // Phase 3: Transfert
            if (!performTransfer(dataOut, in, encryptedData)) {
                System.err.println("Échec du transfert.");
                return false;
            }
            
            System.out.println("Transfert réussi!");
            return true;
            
        } catch (Exception e) {
            System.err.println("Erreur lors du transfert: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // Fermer les ressources
            closeResources(socket, in, out, dataIn, dataOut);
        }
    }
    
    /**
     * Phase 1: Authentification
     * Envoie le login et le password au serveur
     */
    private boolean performAuthentication(PrintWriter out, BufferedReader in) throws IOException {
        out.println(login);
        out.println(password);
        
        String response = in.readLine();
        return "AUTH_OK".equals(response);
    }
    
    /**
     * Phase 2: Négociation
     * Envoie les métadonnées du fichier (nom, taille, hachage)
     */
    private boolean performNegotiation(PrintWriter out, BufferedReader in, String fileName, long fileSize, String hash) throws IOException {
        out.println(fileName);
        out.println(String.valueOf(fileSize));
        out.println(hash);
        
        String response = in.readLine();
        return "READY_FOR_TRANSFER".equals(response);
    }
    
    /**
     * Phase 3: Transfert
     * Envoie les données chiffrées du fichier
     */
    private boolean performTransfer(DataOutputStream dataOut, BufferedReader in, byte[] encryptedData) throws IOException {
        // Envoyer la taille des données chiffrées
        dataOut.writeInt(encryptedData.length);
        
        // Envoyer les données chiffrées
        dataOut.write(encryptedData);
        dataOut.flush();
        
        System.out.println("Données envoyées: " + encryptedData.length + " bytes");
        
        // Attendre la réponse du serveur
        String response = in.readLine();
        return "TRANSFER_SUCCESS".equals(response);
    }
    
    /**
     * Ferme proprement toutes les ressources
     */
    private void closeResources(Socket socket, BufferedReader in, PrintWriter out, 
                                DataInputStream dataIn, DataOutputStream dataOut) {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (dataIn != null) dataIn.close();
            if (dataOut != null) dataOut.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Erreur lors de la fermeture des ressources: " + e.getMessage());
        }
    }
    
    /**
     * Point d'entrée principal
     * Arguments: [serverAddress] [port] [login] [password] [filePath]
     */
    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: SecureFileClient <serverAddress> <port> <login> <password> <filePath>");
            System.out.println("Exemple: SecureFileClient localhost 8888 admin admin123 C:\\fichier.txt");
            System.exit(1);
        }
        
        try {
            String serverAddress = args[0];
            int port = Integer.parseInt(args[1]);
            String login = args[2];
            String password = args[3];
            String filePath = args[4];
            
            SecureFileClient client = new SecureFileClient(serverAddress, port, login, password, filePath);
            boolean success = client.transferFile();
            
            System.exit(success ? 0 : 1);
            
        } catch (NumberFormatException e) {
            System.err.println("Erreur: Le port doit être un nombre.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

