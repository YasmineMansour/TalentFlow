USE talent_flow_db;

CREATE TABLE IF NOT EXISTS offre (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    localisation VARCHAR(255),
    type_contrat VARCHAR(50) DEFAULT 'CDI',
    mode_travail VARCHAR(50) DEFAULT 'ON_SITE',
    salaire_min DOUBLE DEFAULT 0,
    salaire_max DOUBLE DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    statut VARCHAR(50) DEFAULT 'PUBLISHED'
);

CREATE TABLE IF NOT EXISTS avantage (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) DEFAULT 'AUTRE',
    offre_id INT NOT NULL,
    FOREIGN KEY (offre_id) REFERENCES offre(id) ON DELETE CASCADE
);


