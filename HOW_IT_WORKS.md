# Comment fonctionne le projet - Guide détaillé

## Vue d'ensemble

Ce projet implémente un système de transfert de fichiers sécurisé utilisant une architecture **Client-Serveur** basée sur **TCP/IP**. Le client et le serveur peuvent fonctionner sur **des machines différentes avec des adresses IP différentes**, tant qu'ils sont connectés au même réseau ou que le réseau est configuré pour permettre la communication.

---

## Architecture du système

```
┌─────────────────┐                    ┌─────────────────┐
│                 │                    │                 │
│   CLIENT        │  ───TCP/IP───►     │   SERVEUR       │
│                 │                    │                 │
│  (Machine A)    │                    │  (Machine B)    │
│  IP: 192.168.1.5│                    │  IP: 192.168.1.10│
│                 │                    │                 │
└─────────────────┘                    └─────────────────┘
```

---

## Fonctionnement détaillé

### 1. **Le Serveur (SecureFileServer)**

#### Démarrage
- Le serveur démarre et crée un `ServerSocket` qui écoute sur un port (par défaut: 8888)
- Il entre dans une boucle infinie d'attente de connexions
- **Le serveur écoute sur TOUTES les interfaces réseau** (0.0.0.0), ce qui signifie qu'il accepte les connexions de n'importe quelle adresse IP

```java
serverSocket = new ServerSocket(port);  // Écoute sur toutes les interfaces
```

#### Gestion de la concurrence
- Quand un client se connecte, le serveur accepte la connexion avec `serverSocket.accept()`
- **Chaque connexion est immédiatement déléguée à un nouveau thread** (`ClientTransferHandler`)
- Cela permet au serveur de gérer **plusieurs clients simultanément**

```java
Socket clientSocket = serverSocket.accept();  // Bloque jusqu'à une connexion
ClientTransferHandler handler = new ClientTransferHandler(clientSocket);
handler.start();  // Nouveau thread pour ce client
```

---

### 2. **Le Client (SecureFileClient)**

#### Connexion
- Le client se connecte au serveur en spécifiant :
  - **L'adresse IP ou le nom d'hôte** du serveur (ex: `localhost`, `192.168.1.10`, `server.example.com`)
  - **Le port** sur lequel le serveur écoute (ex: `8888`)

```java
socket = new Socket(serverAddress, serverPort);  // Se connecte au serveur
```

**Important** : Le client peut se connecter à n'importe quelle adresse IP accessible sur le réseau !

---

## Protocole de communication en 3 phases

### Phase 1 : Authentification 🔐

**Côté Client :**
1. Envoie le login (ligne de texte)
2. Envoie le password (ligne de texte)
3. Attend la réponse du serveur

**Côté Serveur :**
1. Reçoit le login
2. Reçoit le password
3. Vérifie les identifiants dans la base (classe `Authenticator`)
4. Répond `AUTH_OK` ou `AUTH_FAIL`

**Si AUTH_FAIL** → La connexion est fermée, le transfert s'arrête.

---

### Phase 2 : Négociation 📋

**Côté Client :**
1. Lit le fichier local
2. Calcule le hachage SHA-256 du fichier original
3. Chiffre le fichier avec AES-256
4. Envoie les métadonnées :
   - Nom du fichier
   - Taille en bytes (du fichier original)
   - Hachage SHA-256 (du fichier original)
5. Attend la réponse `READY_FOR_TRANSFER`

**Côté Serveur :**
1. Reçoit les métadonnées
2. Stocke les informations dans un objet `FileMetadata`
3. Répond `READY_FOR_TRANSFER` pour indiquer qu'il est prêt à recevoir

---

### Phase 3 : Transfert et Vérification 📦

**Côté Client :**
1. Envoie la taille des données chiffrées (4 bytes, int)
2. Envoie les données chiffrées (tableau de bytes)
3. Attend la réponse finale

**Côté Serveur :**
1. Reçoit la taille des données chiffrées
2. Reçoit toutes les données chiffrées (lecture par chunks)
3. **Déchiffre** les données avec la même clé AES
4. **Vérifie l'intégrité** :
   - Calcule le SHA-256 des données déchiffrées
   - Compare avec le hachage reçu en Phase 2
5. Si les hachages correspondent :
   - Enregistre le fichier dans `uploads/`
   - Répond `TRANSFER_SUCCESS`
6. Sinon, répond `TRANSFER_FAIL`

---

## Communication sur différentes adresses IP

### ✅ OUI, le client et le serveur peuvent fonctionner sur des IP différentes !

Le système utilise **TCP/IP**, qui est conçu pour la communication entre machines sur un réseau.

### Scénarios possibles :

#### 1. **Même machine (localhost)**
```bash
# Serveur
java -cp bin com.securefiletransfer.SecureFileServer

# Client
java -cp bin com.securefiletransfer.SecureFileClient localhost 8888 admin admin123 file.txt
```

#### 2. **Même réseau local (LAN)**
```bash
# Serveur sur Machine A (IP: 192.168.1.10)
java -cp bin com.securefiletransfer.SecureFileServer 8888

# Client sur Machine B (IP: 192.168.1.5)
java -cp bin com.securefiletransfer.SecureFileClient 192.168.1.10 8888 admin admin123 file.txt
```

#### 3. **Réseaux différents (Internet)**
```bash
# Serveur sur Machine A (IP publique: 203.0.113.5)
# (Le serveur doit être accessible depuis Internet)

# Client sur Machine B (n'importe où sur Internet)
java -cp bin com.securefiletransfer.SecureFileClient 203.0.113.5 8888 admin admin123 file.txt
```

### ⚠️ Points importants pour la communication réseau :

1. **Firewall** : Le port du serveur (8888) doit être ouvert dans le firewall
2. **NAT/Router** : Si le serveur est derrière un routeur, il faut configurer le port forwarding
3. **Adresse IP** : Utiliser l'adresse IP réelle du serveur, pas `localhost`

### Comment trouver l'adresse IP du serveur :

**Windows :**
```cmd
ipconfig
```
Cherchez "Adresse IPv4" (ex: 192.168.1.10)

**Linux/Mac :**
```bash
ifconfig
# ou
ip addr
```

---

## Flux de données complet

```
CLIENT                                    SERVEUR
  │                                         │
  │  ──── Connexion TCP ──────────────────► │
  │                                         │
  │  ──── login ──────────────────────────► │
  │  ──── password ───────────────────────► │
  │  ◄─── AUTH_OK ───────────────────────── │
  │                                         │
  │  [Calcule SHA-256]                      │
  │  [Chiffre avec AES]                     │
  │                                         │
  │  ──── fileName ───────────────────────► │
  │  ──── fileSize ───────────────────────► │
  │  ──── hash ───────────────────────────► │
  │  ◄─── READY_FOR_TRANSFER ────────────── │
  │                                         │
  │  ──── encryptedSize (int) ────────────► │
  │  ──── encryptedData (bytes) ──────────► │
  │                                         │
  │                                         │ [Déchiffre]
  │                                         │ [Vérifie SHA-256]
  │                                         │ [Enregistre fichier]
  │  ◄─── TRANSFER_SUCCESS ──────────────── │
  │                                         │
  │  [Ferme connexion]                      │ [Ferme connexion]
```

---

## Sécurité

### Chiffrement
- **Algorithme** : AES-256
- **Clé partagée** : La même clé est utilisée par le client et le serveur (définie dans `CryptoUtils.getDefaultKey()`)
- **Important** : En production, cette clé devrait être échangée de manière sécurisée (Diffie-Hellman, RSA, etc.)

### Intégrité
- **Hachage SHA-256** : Garantit que le fichier n'a pas été modifié pendant le transfert
- Vérification effectuée après déchiffrement

### Authentification
- **Login/Password** : Seuls les utilisateurs autorisés peuvent transférer des fichiers
- Stockage actuel : En dur dans `Authenticator` (à améliorer en production)

---

## Exemple pratique complet

### Étape 1 : Démarrer le serveur (Machine A - IP: 192.168.1.10)

```bash
cd C:\Users\HP\Documents\Java Avancé\Project
compile.bat
run-server.bat
```

**Sortie :**
```
Serveur démarré sur le port 8888
En attente de connexions...
```

### Étape 2 : Envoyer un fichier depuis le client (Machine B - IP: 192.168.1.5)

```bash
java -cp bin com.securefiletransfer.SecureFileClient 192.168.1.10 8888 admin admin123 C:\monfichier.txt
```

**Sortie côté client :**
```
Connexion au serveur 192.168.1.10:8888...
Connecté au serveur.
Authentification réussie.
Hachage SHA-256 calculé: a1b2c3d4e5f6...
Chiffrement du fichier...
Fichier chiffré: 1024 bytes -> 1040 bytes
Négociation réussie.
Données envoyées: 1040 bytes
Transfert réussi!
```

**Sortie côté serveur :**
```
Client connecté: /192.168.1.5:54321
Authentification réussie pour: admin
Négociation réussie - Fichier: monfichier.txt, Taille: 1024 bytes
Données chiffrées reçues: 1040 bytes
Données déchiffrées: 1024 bytes
Fichier enregistré avec succès: uploads\monfichier.txt
Connexion fermée avec /192.168.1.5:54321
```

---

## Résumé

✅ **Le client et le serveur peuvent fonctionner sur des machines différentes**  
✅ **Ils communiquent via TCP/IP sur le réseau**  
✅ **Le serveur écoute sur toutes les interfaces réseau**  
✅ **Le client se connecte en spécifiant l'adresse IP du serveur**  
✅ **Plusieurs clients peuvent se connecter simultanément**  
✅ **Le transfert est sécurisé avec AES-256 et vérifié avec SHA-256**

