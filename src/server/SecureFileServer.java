package server;

import crypto.KeyManager;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class SecureFileServer {
    
    private static final int DEFAULT_PORT = 8888;
    private ServerSocket serverSocket;
    private boolean running = false;
    
    public SecureFileServer(int port) throws Exception {
        
        SecretKey key = KeyManager.loadOrGenerateKey();
        System.out.println("Clé AES chargée/générée avec succès");
        

        serverSocket = new ServerSocket(port);
        System.out.println("Serveur démarré sur le port " + port);
        System.out.println("Adresse IP locale: " + getLocalIP());
        System.out.println("En attente de connexions...");
    }
    
    public void start() {
        running = true;
        
        while (running) {
            try {
               
                Socket clientSocket = serverSocket.accept();    
               
                ClientTransferHandler handler = new ClientTransferHandler(clientSocket);
                handler.start();
                
            } catch (IOException e) {
                if (running) {
                    System.err.println("Erreur lors de l'acceptation d'une connexion: " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la création du handler: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la fermeture du serveur: " + e.getMessage());
        }
    }
    
    private String getLocalIP() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
    
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Port invalide, utilisation du port par défaut: " + DEFAULT_PORT);
            }
        }
        
        try {
            SecureFileServer server = new SecureFileServer(port);
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nArrêt du serveur...");
                server.stop();
            }));
            
            server.start();
            
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


