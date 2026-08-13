CREATE DATABASE IF NOT EXISTS travel_management;
USE travel_management;

CREATE TABLE IF NOT EXISTS Customer(
 Customer_ID INT PRIMARY KEY AUTO_INCREMENT,
 Customer_Name VARCHAR(100) NOT NULL,
 Gender VARCHAR(10),
 Email VARCHAR(100) UNIQUE,
 Phone_No VARCHAR(15),
 Address VARCHAR(255),
 City VARCHAR(50),
 Username VARCHAR(50) UNIQUE,
 Password VARCHAR(100) NOT NULL,
 Registration_Date DATE
);

CREATE TABLE IF NOT EXISTS TourPackage(
 Package_ID INT PRIMARY KEY AUTO_INCREMENT,
 Package_Name VARCHAR(100) NOT NULL,
 Destination VARCHAR(100),
 Duration INT,
 Price DECIMAL(10,2),
 Description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS Booking(
 Booking_ID INT PRIMARY KEY AUTO_INCREMENT,
 Customer_ID INT,
 Package_ID INT,
 Booking_Date DATE,
 Number_Of_Persons INT,
 Total_Amount DECIMAL(10,2),
 Booking_Status VARCHAR(20),
 FOREIGN KEY(Customer_ID) REFERENCES Customer(Customer_ID),
 FOREIGN KEY(Package_ID) REFERENCES TourPackage(Package_ID)
);

CREATE TABLE IF NOT EXISTS Feedback(
 Feedback_ID INT PRIMARY KEY AUTO_INCREMENT,
 Customer_ID INT,
 Feedback_Text VARCHAR(255),
 Rating INT,
 Feedback_Date DATE,
 FOREIGN KEY(Customer_ID) REFERENCES Customer(Customer_ID)
);

INSERT INTO TourPackage(Package_Name,Destination,Duration,Price,Description)
SELECT 'Goa Beach Tour','Goa',5,15000,'5 Days and 4 Nights'
WHERE NOT EXISTS (SELECT 1 FROM TourPackage WHERE Package_Name='Goa Beach Tour');

INSERT INTO TourPackage(Package_Name,Destination,Duration,Price,Description)
SELECT 'Manali Adventure','Manali',6,18000,'6 Days Adventure Tour'
WHERE NOT EXISTS (SELECT 1 FROM TourPackage WHERE Package_Name='Manali Adventure');

INSERT INTO TourPackage(Package_Name,Destination,Duration,Price,Description)
SELECT 'Rajasthan Heritage','Rajasthan',7,22000,'7 Days Heritage Tour'
WHERE NOT EXISTS (SELECT 1 FROM TourPackage WHERE Package_Name='Rajasthan Heritage');