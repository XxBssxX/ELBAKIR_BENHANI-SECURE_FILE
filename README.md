# Système de Transfert de Fichiers Sécurisé

Application Client-Serveur pour le transfert de fichiers sécurisé utilisant le protocole TCP avec chiffrement AES et vérification d'intégrité SHA-256.

## Architecture

Le système est composé de plusieurs composants :

- **SecureFileServer** : Serveur principal qui écoute les connexions et délègue chaque client à un thread dédié
- **ClientTransferHandler** : Thread gérant la communication avec un client spécifique (3 phases du protocole)
- **SecureFileClient** : Client en ligne de commande pour envoyer des fichiers
- **CryptoUtils** : Utilitaires pour le chiffrement AES et le hachage SHA-256
- **Authenticator** : Gestion de l'authentification des utilisateurs

## Protocole de Communication

Le système implémente un protocole en 3 phases :

### Phase 1 : Authentification
- Le client envoie son login et password
- Le serveur vérifie les identifiants et répond `AUTH_OK` ou `AUTH_FAIL`

### Phase 2 : Négociation
- Le client envoie les métadonnées du fichier :
  - Nom du fichier
  - Taille en bytes
  - Hachage SHA-256
- Le serveur répond `READY_FOR_TRANSFER`

### Phase 3 : Transfert et Vérification
- Le client envoie les données chiffrées du fichier
- Le serveur :
  - Reçoit les données chiffrées
  - Les déchiffre avec AES
  - Vérifie l'intégrité avec le hachage SHA-256
  - Enregistre le fichier dans le dossier `uploads/`
  - Répond `TRANSFER_SUCCESS` ou `TRANSFER_FAIL`

## Sécurité

### Chiffrement
- **Algorithme** : AES (Advanced Encryption Standard)
- **Mode** : AES/ECB/PKCS5Padding
- **Taille de clé** : 256 bits
- **Gestion de la clé** : Clé prédéfinie partagée entre client et serveur (voir section "Améliorations possibles")

### Intégrité
- **Algorithme de hachage** : SHA-256
- Vérification de l'intégrité avant et après le transfert

## Compilation

### Prérequis
- Java JDK 8 ou supérieur

### Compilation du projet

```bash
# Compiler tous les fichiers Java
javac -d bin src/com/securefiletransfer/*.java
```

## Utilisation

### 1. Démarrer le serveur

```bash
# Depuis le répertoire racine du projet
java -cp bin com.securefiletransfer.SecureFileServer [port]

# Exemple (port par défaut: 8888)
java -cp bin com.securefiletransfer.SecureFileServer
# ou avec un port personnalisé
java -cp bin com.securefiletransfer.SecureFileServer 9999
```

Le serveur affichera :
```
Serveur démarré sur le port 8888
En attente de connexions...
```

### 2. Envoyer un fichier avec le client

```bash
# Syntaxe
java -cp bin com.securefiletransfer.SecureFileClient <serverAddress> <port> <login> <password> <filePath>

# Exemple
java -cp bin com.securefiletransfer.SecureFileClient localhost 8888 admin admin123 C:\monfichier.txt
```

### Utilisateurs par défaut

Le système inclut quelques utilisateurs de test :
- `admin` / `admin123`
- `user1` / `password1`
- `test` / `test123`

## Structure du Projet

```
Project/
├── src/
│   └── com/
│       └── securefiletransfer/
│           ├── SecureFileServer.java      # Serveur principal
│           ├── ClientTransferHandler.java # Handler pour chaque client
│           ├── SecureFileClient.java      # Client CLI
│           ├── CryptoUtils.java           # Utilitaires cryptographiques
│           └── Authenticator.java         # Gestion de l'authentification
├── bin/                                    # Fichiers compilés (.class)
├── uploads/                                # Dossier de réception (créé automatiquement)
└── README.md                               # Ce fichier
```

## Fonctionnalités

✅ **Concurrence** : Le serveur gère plusieurs clients simultanément grâce aux threads  
✅ **Authentification** : Système de login/password  
✅ **Chiffrement** : Transfert de fichiers chiffrés avec AES  
✅ **Intégrité** : Vérification SHA-256 pour garantir l'intégrité des fichiers  
✅ **Protocole structuré** : Communication en 3 phases bien définies  
✅ **Gestion d'erreurs** : Gestion robuste des erreurs et fermeture propre des connexions  

## Améliorations Possibles

### Sécurité
1. **Échange de clés sécurisé** : Implémenter un échange de clés Diffie-Hellman ou RSA pour éviter d'avoir une clé prédéfinie
2. **Authentification renforcée** : Utiliser des mots de passe hashés (bcrypt, Argon2) au lieu de stockage en clair
3. **Mode de chiffrement** : Remplacer ECB par CBC ou GCM (plus sécurisé)
4. **TLS/SSL** : Ajouter une couche TLS pour sécuriser la connexion TCP

### Fonctionnalités
1. **Base de données** : Remplacer le stockage en dur des utilisateurs par une base de données
2. **Interface graphique** : Créer une interface graphique pour le client
3. **Transfert de plusieurs fichiers** : Permettre le transfert de plusieurs fichiers en une session
4. **Progression du transfert** : Afficher une barre de progression pour les gros fichiers
5. **Gestion des permissions** : Système de permissions pour les utilisateurs

## Notes Techniques

- Le serveur crée automatiquement le dossier `uploads/` pour stocker les fichiers reçus
- Les fichiers sont déchiffrés avant d'être enregistrés
- La vérification d'intégrité se fait après le déchiffrement
- Le serveur peut être arrêté proprement avec Ctrl+C

## Auteur

Projet réalisé dans le cadre du cours Java Avancé.

