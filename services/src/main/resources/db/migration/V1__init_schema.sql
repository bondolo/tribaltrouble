CREATE TABLE IF NOT EXISTS registrations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reg_key VARCHAR(20) DEFAULT NULL,
    disabled BOOLEAN NOT NULL DEFAULT FALSE,
    banned BOOLEAN NOT NULL DEFAULT FALSE,
    username VARCHAR(40) NOT NULL UNIQUE,
    email VARCHAR(60),
    password VARCHAR(40),
    last_used_profile VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS profiles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reg_id INT,
    nick VARCHAR(40) NOT NULL UNIQUE,
    rating INT NOT NULL DEFAULT 1000,
    wins INT NOT NULL DEFAULT 0,
    losses INT NOT NULL DEFAULT 0,
    invalid INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS deleted_profiles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reg_id INT,
    nick VARCHAR(40),
    rating INT,
    wins INT,
    losses INT,
    invalid INT
);

CREATE TABLE IF NOT EXISTS settings (
    property VARCHAR(50) PRIMARY KEY,
    value VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS games (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player1_name VARCHAR(40),
    player1_id INT,
    player1_race VARCHAR(10),
    player1_team VARCHAR(10),
    player2_name VARCHAR(40),
    player2_race VARCHAR(10),
    player2_team VARCHAR(10),
    player3_name VARCHAR(40),
    player3_race VARCHAR(10),
    player3_team VARCHAR(10),
    player4_name VARCHAR(40),
    player4_race VARCHAR(10),
    player4_team VARCHAR(10),
    player5_name VARCHAR(40),
    player5_race VARCHAR(10),
    player5_team VARCHAR(10),
    player6_name VARCHAR(40),
    player6_race VARCHAR(10),
    player6_team VARCHAR(10),
    time_create TIMESTAMP,
    time_start TIMESTAMP,
    time_stop TIMESTAMP,
    name VARCHAR(40),
    rated VARCHAR(5),
    speed INT,
    size INT,
    hills INT,
    trees INT,
    resources INT,
    mapcode VARCHAR(40),
    status VARCHAR(20),
    winner INT
);

CREATE TABLE IF NOT EXISTS game_reports (
    game_id INT,
    tick INT,
    team1 INT,
    team2 INT,
    team3 INT,
    team4 INT,
    team5 INT,
    team6 INT
);

CREATE TABLE IF NOT EXISTS connections (
    game_id INT,
    nick1 VARCHAR(40),
    nick2 VARCHAR(40),
    priority INT
);

CREATE TABLE IF NOT EXISTS online_profiles (
    nick VARCHAR(40) PRIMARY KEY,
    game_id INT
);

CREATE TABLE IF NOT EXISTS messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    time TIMESTAMP,
    message TEXT
);
