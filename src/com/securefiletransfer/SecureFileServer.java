package com.securefiletransfer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Serveur principal pour le transfert de fichiers sécurisé
 * Écoute sur un port et délègue chaque connexion à un thread ClientTransferHandler
 */
public class SecureFileServer {
    
    private static final int DEFAULT_PORT = 8888;
    private ServerSocket serverSocket;
    private boolean running = false;
    
    /**
     * Démarre le serveur sur le port spécifié
     */
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Serveur démarré sur le port " + port);
        System.out.println("En attente de connexions...");
        
        while (running) {
            try {
                // Accepter une nouvelle connexion
                Socket clientSocket = serverSocket.accept();
                
                // Déléguer le traitement à un nouveau thread
                ClientTransferHandler handler = new ClientTransferHandler(clientSocket);
                handler.start();
                
            } catch (IOException e) {
                if (running) {
                    System.err.println("Erreur lors de l'acceptation d'une connexion: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Arrête le serveur
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("Serveur arrêté.");
        } catch (IOException e) {
            System.err.println("Erreur lors de l'arrêt du serveur: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        // Permettre de spécifier le port en argument
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Port invalide, utilisation du port par défaut: " + DEFAULT_PORT);
            }
        }
        
        SecureFileServer server = new SecureFileServer();
        
        // Gérer l'arrêt propre avec Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nArrêt du serveur...");
            server.stop();
        }));
        
        try {
            server.start(port);
        } catch (IOException e) {
            System.err.println("Impossible de démarrer le serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

