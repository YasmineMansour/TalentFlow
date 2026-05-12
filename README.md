
```markdown
# Application de Gestion d'Authentification

## 📋 Description

Application JavaFX de gestion d'authentification avec fonctionnalités de connexion et de réinitialisation de mot de passe. L'application utilise une base de données SQL pour stocker les informations utilisateur et intègre un système de vérification par code à 6 chiffres.

## 🛠️ Technologies

- **Langage**: Java
- **Build**: Maven
- **Interface**: JavaFX 17
- **Base de données**: SQL
- **IDE**: IntelliJ IDEA 2025.2.2

## 📁 Structure du Projet

```
src/main/
├── java/
│   └── org/example/
│       ├── GUI/
│       │   ├── ResetPasswordController.java
│       │   └── LoginController.java
│       └── Database/
│           └── DatabaseManager.java
└── resources/
    └── org/example/
        ├── ResetPasswordView.fxml
        ├── LoginView.fxml
        └── styles.css
```

## ✨ Fonctionnalités

### 1. **Réinitialisation de Mot de Passe**
- Saisie d'un code de vérification (6 chiffres)
- Création d'un nouveau mot de passe avec validation
- Confirmation du mot de passe
- Affichage/masquage du mot de passe (boutons 👁)
- Messages d'état pour l'utilisateur

### 2. **Validation du Mot de Passe**
- Minimum 8 caractères
- Au moins 1 lettre majuscule
- Au moins 1 chiffre
- Au moins 1 symbole spécial

### 3. **Interface Utilisateur**
- Design moderne avec gradient de couleurs
- Responsive et centré
- Thème vert (#00b894 - #55efc4)
- Navigation fluide

## 🎨 Composants FXML

### Écran de Réinitialisation
- **Panel gauche**: Section décorée avec instructions
- **Panel droit**: Formulaire de réinitialisation
  - Champ code de vérification
  - Champ nouveau mot de passe avec toggle
  - Champ confirmation avec toggle
  - Bouton de réinitialisation
  - Lien retour connexion

## 🔐 Sécurité

- Mots de passe hashés en base de données
- Validation côté client et serveur
- Code de vérification unique par utilisateur
- Messages d'erreur génériques

## 📦 Dépendances Maven

```xml
<!-- JavaFX -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>17</version>
</dependency>

<!-- Base de données -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.x</version>
</dependency>
```

## 🚀 Installation

1. Cloner le projet depuis le repository Git
2. Configurer les identifiants de base de données
3. Compiler avec Maven: `mvn clean install`
4. Exécuter l'application

## 🗄️ Base de Données

### Tables requises

```sql
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE verification_codes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## 📝 Fichiers Clés

- `ResetPasswordView.fxml`: Interface de réinitialisation
- `ResetPasswordController.java`: Logique de contrôle
- `styles.css`: Feuille de style
- `DatabaseManager.java`: Gestion des requêtes SQL

## ⚙️ Configuration

Créer un fichier `config.properties` avec:
```properties
db.url=jdbc:mysql://localhost:3306/auth_db
db.username=root
db.password=YOUR_PASSWORD
```

## 📧 Contact & Support

Développeur: Mounib-krimi

---

**Version**: 1.0  
**Dernière mise à jour**: 2026
```

