CREATE DATABASE IF NOT EXISTS agri_rental;
USE agri_rental;

CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role ENUM('FARMER','OWNER','ADMIN') NOT NULL
);

CREATE TABLE IF NOT EXISTS equipment (
    equipment_id INT PRIMARY KEY AUTO_INCREMENT,
    owner_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    daily_rate DECIMAL(10,2) NOT NULL,
    location VARCHAR(150) NOT NULL,
    status ENUM('AVAILABLE','MAINTENANCE','INACTIVE') NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT fk_equipment_owner FOREIGN KEY (owner_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS bookings (
    booking_id INT PRIMARY KEY AUTO_INCREMENT,
    farmer_id INT NOT NULL,
    equipment_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING','APPROVED','REJECTED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_farmer FOREIGN KEY (farmer_id) REFERENCES users(user_id),
    CONSTRAINT fk_booking_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id),
    CONSTRAINT chk_booking_dates CHECK (end_date >= start_date)
);

CREATE TABLE IF NOT EXISTS rental_payments (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    status ENUM('PENDING','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id)
);

CREATE TABLE IF NOT EXISTS maintenance_records (
    maintenance_id INT PRIMARY KEY AUTO_INCREMENT,
    equipment_id INT NOT NULL,
    service_date DATE NOT NULL,
    description VARCHAR(500) NOT NULL,
    cost DECIMAL(10,2) NOT NULL DEFAULT 0,
    status ENUM('OPEN','IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'OPEN',
    CONSTRAINT fk_maintenance_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id)
);
