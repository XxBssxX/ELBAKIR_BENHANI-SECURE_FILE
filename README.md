 # Système de Transfert de Fichiers Sécurisé

Application Client-Serveur pour le transfert de fichiers sécurisé utilisant le protocole TCP avec chiffrement AES et vérification d'intégrité SHA-256.

## Architecture

Le système est composé de plusieurs composants :

- **SecureFileServer** : Serveur principal qui écoute les connexions et délègue chaque client à un thread dédié
- **ClientTransferHandler** : Thread gérant la communication avec un client spécifique (3 phases du protocole)
- **SecureFileClient** : Client en ligne de commande pour envoyer des fichiers
- **CryptoUtils** : Utilitaires pour le chiffrement AES et le hachage SHA-256

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
  - Enregistre le fichier (préfixe `received_`)
  - Répond `TRANSFER_SUCCESS` ou `TRANSFER_FAIL`

## Sécurité

### Chiffrement
- **Algorithme** : AES (Advanced Encryption Standard)
- **Mode** : AES/ECB/PKCS5Padding
- **Taille de clé** : 256 bits
- **Gestion de la clé** : Clé prédéfinie partagée entre client et serveur via `shared_key.txt`

### Intégrité
- **Algorithme de hachage** : SHA-256
- Vérification de l'intégrité après déchiffrement

## Guide d’exécution

Toutes les commandes sont à lancer dans le terminal PowerShell.

### 1. Compilation (chaque machine Java)
```powershell
cd "C:\Users\HP\Documents\Java Avancé\Système de transfert de fichiers sécurisé"
if (!(Test-Path bin)) { New-Item -ItemType Directory -Path bin | Out-Null }
$sources = Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -d bin -encoding UTF-8 $sources
```

### 2. Lancer le serveur (machine locale)
```powershell
cd "C:\Users\HP\Documents\Java Avancé\Système de transfert de fichiers sécurisé\bin"
java server.SecureFileServer 8888
```
Laissez cette fenêtre ouverte. Notez l’adresse IP (via `ipconfig` si besoin). Copiez `shared_key.txt` vers les clients distants.

### 3. Lancer le client (même machine ou distante)
```powershell
cd "C:\Users\HP\Documents\Java Avancé\Système de transfert de fichiers sécurisé\bin"
java client.SecureFileClient
```
Le client demande l’IP du serveur (ex. `127.0.0.1` ou une IPv4 du réseau), le port (`8888`), le login/mot de passe et un chemin absolu vers le fichier à envoyer.

## Structure du Projet

```
Project/
├── src/
│   ├── server/
│   │   ├── SecureFileServer.java
│   │   ├── ClientTransferHandler.java
│   │   └── UserCredentials.java
│   ├── client/
│   │   └── SecureFileClient.java
│   └── crypto/
│       ├── CryptoUtils.java
│       └── KeyManager.java
├── bin/                   # Fichiers compilés (.class)
├── shared_key.txt         # Clé AES partagée
└── README.md
```

## Fonctionnalités

✅ **Concurrence** : threads par client  
✅ **Authentification** : login/password  
✅ **Chiffrement** : AES-256 (ECB/PKCS5Padding)  
✅ **Intégrité** : SHA-256  
✅ **Protocole structuré** : 3 phases  

## Améliorations possibles

- Échange de clé sécurisé (Diffie-Hellman/RSA)
- Stockage sécurisé des mots de passe (hachage)
- Mode de chiffrement plus robuste (CBC ou GCM)
- TLS/SSL pour la connexion TCP

## GROUPE
Projet réalisé par **ELBAKIR Bassam** et **BENHANI Mohammed**  
Encadrant : **Mr. BENTAJER Ahmed**

### Identifiants de test
```
bassam    bassam123
mohammed  mohammed123
```
