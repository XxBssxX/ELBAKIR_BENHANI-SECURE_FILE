package com.securefiletransfer;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Classe utilitaire pour les opérations cryptographiques
 * Gère le chiffrement AES et le hachage SHA-256
 */
public class CryptoUtils {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int KEY_SIZE = 256; // 256 bits pour AES-256
    
    /**
     * Génère une clé AES aléatoire
     * @return SecretKey générée
     */
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE);
        return keyGenerator.generateKey();
    }
    
    /**
     * Convertit une clé en String Base64 pour le stockage/transmission
     */
    public static String keyToString(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    /**
     * Reconstruit une clé à partir d'une String Base64
     */
    public static SecretKey stringToKey(String keyString) {
        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        return new SecretKeySpec(decodedKey, ALGORITHM);
    }
    
    /**
     * Chiffre les données avec AES
     * @param data Données à chiffrer
     * @param key Clé secrète
     * @return Données chiffrées
     */
    public static byte[] encrypt(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }
    
    /**
     * Déchiffre les données avec AES
     * @param encryptedData Données chiffrées
     * @param key Clé secrète
     * @return Données déchiffrées
     */
    public static byte[] decrypt(byte[] encryptedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(encryptedData);
    }
    
    /**
     * Calcule le hachage SHA-256 d'un fichier
     * @param data Données du fichier
     * @return Hachage SHA-256 en hexadécimal
     */
    public static String calculateSHA256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        
        // Convertir en hexadécimal
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Génère une clé AES prédéfinie pour le projet
     * Cette clé sera partagée entre client et serveur
     * En production, cette clé devrait être échangée de manière sécurisée
     * (ex: Diffie-Hellman, RSA, ou clé partagée via canal sécurisé)
     */
    public static SecretKey getDefaultKey() throws Exception {
        // Clé prédéfinie (en production, utiliser un échange de clés sécurisé)
        // Pour AES-256, nous avons besoin d'exactement 32 bytes
        String defaultKeyString = "MySecretKey123456789012345678901234567890"; // 40 caractères
        // Convertir en bytes avec encodage UTF-8 explicite et prendre les 32 premiers bytes
        byte[] keyBytes = new byte[32];
        byte[] defaultBytes = defaultKeyString.getBytes("UTF-8");
        System.arraycopy(defaultBytes, 0, keyBytes, 0, Math.min(32, defaultBytes.length));
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
}

