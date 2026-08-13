# Travel and Tour Management System

A simple full-stack academic project using **Core Java**, **HTML/CSS**, and **MySQL**.

## Features
- Customer registration
- Customer login
- Tour package management
- Tour booking
- Booking history
- Booking cancellation
- Feedback
- MySQL database integration using JDBC
- Java built-in HTTP server

## Requirements
- JDK 11+
- MySQL 8+
- MySQL Connector/J JAR

## Setup

1. Create the database:
   ```sql
   SOURCE schema.sql;
   ```

2. Download MySQL Connector/J and place the JAR in the project folder.

3. Open `TravelTourApp.java` and change:
   ```java
   DB_USER = "root";
   DB_PASSWORD = "your_mysql_password";
   ```

4. Compile on Windows:
   ```bash
   javac -cp ".;mysql-connector-j-<version>.jar" TravelTourApp.java
   ```

5. Run:
   ```bash
   java -cp ".;mysql-connector-j-<version>.jar" TravelTourApp
   ```

6. Open:
   `http://localhost:8080`

## GitHub

Do not upload your real MySQL password. Replace it before pushing, or use environment variables for production.

## Project Structure
```text
Travel-Tour-Management-System/
├── TravelTourApp.java
├── index.html
├── style.css
├── schema.sql
└── README.md
```
