package com.securefiletransfer;

import java.util.HashMap;
import java.util.Map;

/**
 * Gère l'authentification des utilisateurs
 * Stocke les identifiants en dur (pour la démonstration)
 * En production, utiliser une base de données ou un système d'authentification sécurisé
 */
public class Authenticator {
    
    // Map des utilisateurs autorisés (login -> password)
    private static final Map<String, String> users = new HashMap<>();
    
    static {
        // Initialiser avec quelques utilisateurs de test
        users.put("admin", "admin123");
        users.put("user1", "password1");
        users.put("test", "test123");
    }
    
    /**
     * Vérifie si le couple login/password est valide
     * @param login Nom d'utilisateur
     * @param password Mot de passe
     * @return true si les identifiants sont valides, false sinon
     */
    public static boolean authenticate(String login, String password) {
        String storedPassword = users.get(login);
        return storedPassword != null && storedPassword.equals(password);
    }
    
    /**
     * Ajoute un nouvel utilisateur (pour tests)
     */
    public static void addUser(String login, String password) {
        users.put(login, password);
    }
}

