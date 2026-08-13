import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;

public class TravelTourApp {
    static final int PORT = 8080;
    static final String DB_URL = "jdbc:mysql://localhost:3306/travel_management";
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "your_mysql_password";

    public static void main(String[] args) throws Exception {
        Database.initialize();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", TravelTourApp::home);
        server.createContext("/style.css", TravelTourApp::css);
        server.createContext("/register", TravelTourApp::register);
        server.createContext("/login", TravelTourApp::login);
        server.createContext("/book", TravelTourApp::book);
        server.createContext("/bookings", TravelTourApp::bookings);
        server.createContext("/cancel", TravelTourApp::cancel);
        server.createContext("/feedback", TravelTourApp::feedback);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("Travel & Tour Management System");
        System.out.println("Open: http://localhost:" + PORT);
    }

    static void home(HttpExchange e) throws IOException {
        String html = Files.readString(Path.of("index.html"));
        send(e, html, "text/html");
    }

    static void css(HttpExchange e) throws IOException {
        send(e, Files.readString(Path.of("style.css")), "text/css");
    }

    static void register(HttpExchange e) throws IOException {
        Map<String,String> f = form(e);
        String sql = "INSERT INTO Customer(Customer_Name,Gender,Email,Phone_No,Address,City,Username,Password,Registration_Date) VALUES(?,?,?,?,?,?,?,?,CURDATE())";
        try (Connection c=Database.getConnection();
             PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,f.get("name")); p.setString(2,f.get("gender"));
            p.setString(3,f.get("email")); p.setString(4,f.get("phone"));
            p.setString(5,f.get("address")); p.setString(6,f.get("city"));
            p.setString(7,f.get("username")); p.setString(8,f.get("password"));
            p.executeUpdate();
            page(e,"Registration Successful","Your account has been created.");
        } catch(SQLException ex) {
            page(e,"Registration Failed",ex.getMessage());
        }
    }

    static void login(HttpExchange e) throws IOException {
        Map<String,String> f=form(e);
        String sql="SELECT Customer_Name FROM Customer WHERE Username=? AND Password=?";
        try(Connection c=Database.getConnection(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,f.get("username")); p.setString(2,f.get("password"));
            ResultSet r=p.executeQuery();
            if(r.next()) page(e,"Login Successful","Welcome, "+esc(r.getString(1))+"!");
            else page(e,"Login Failed","Invalid username or password.");
        } catch(SQLException ex){ page(e,"Database Error",ex.getMessage()); }
    }

    static void book(HttpExchange e) throws IOException {
        Map<String,String> f=form(e);
        int customerId=Integer.parseInt(f.get("customerId"));
        int packageId=Integer.parseInt(f.get("packageId"));
        int persons=Integer.parseInt(f.get("persons"));

        String priceSql="SELECT Price,Package_Name FROM TourPackage WHERE Package_ID=?";
        String insert="INSERT INTO Booking(Customer_ID,Package_ID,Booking_Date,Number_Of_Persons,Total_Amount,Booking_Status) VALUES(?,?,CURDATE(),?,?,'Confirmed')";
        try(Connection c=Database.getConnection();
            PreparedStatement q=c.prepareStatement(priceSql);
            PreparedStatement p=c.prepareStatement(insert,Statement.RETURN_GENERATED_KEYS)) {
            q.setInt(1,packageId);
            ResultSet r=q.executeQuery();
            if(!r.next()){page(e,"Booking Failed","Package not found.");return;}
            double total=r.getDouble("Price")*persons;
            p.setInt(1,customerId); p.setInt(2,packageId); p.setInt(3,persons); p.setDouble(4,total);
            p.executeUpdate();
            ResultSet keys=p.getGeneratedKeys();
            int bookingId=keys.next()?keys.getInt(1):0;
            page(e,"Booking Confirmed","Booking ID: "+bookingId+"<br>Total Amount: ₹"+String.format("%.2f",total));
        } catch(SQLException ex){page(e,"Booking Failed",ex.getMessage());}
    }

    static void bookings(HttpExchange e) throws IOException {
        Map<String,String> f=form(e);
        int customerId=Integer.parseInt(f.get("customerId"));
        String sql="SELECT b.Booking_ID,t.Package_Name,b.Booking_Date,b.Number_Of_Persons,b.Total_Amount,b.Booking_Status FROM Booking b JOIN TourPackage t ON b.Package_ID=t.Package_ID WHERE b.Customer_ID=? ORDER BY b.Booking_ID DESC";
        StringBuilder out=new StringBuilder("<h2>My Bookings</h2><table><tr><th>ID</th><th>Package</th><th>Date</th><th>Persons</th><th>Amount</th><th>Status</th></tr>");
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,customerId); ResultSet r=p.executeQuery();
            while(r.next()) out.append("<tr><td>").append(r.getInt(1)).append("</td><td>").append(esc(r.getString(2)))
                .append("</td><td>").append(r.getDate(3)).append("</td><td>").append(r.getInt(4))
                .append("</td><td>₹").append(r.getDouble(5)).append("</td><td>").append(esc(r.getString(6))).append("</td></tr>");
            out.append("</table><p><a href='/'>Back</a></p>"); page(e,"Bookings",out.toString());
        } catch(SQLException ex){page(e,"Error",ex.getMessage());}
    }

    static void cancel(HttpExchange e) throws IOException {
        Map<String,String> f=form(e);
        String sql="UPDATE Booking SET Booking_Status='Cancelled' WHERE Booking_ID=? AND Customer_ID=?";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,Integer.parseInt(f.get("bookingId")));p.setInt(2,Integer.parseInt(f.get("customerId")));
            page(e,"Cancellation",p.executeUpdate()>0?"Booking cancelled.":"Booking not found.");
        }catch(SQLException ex){page(e,"Error",ex.getMessage());}
    }

    static void feedback(HttpExchange e) throws IOException {
        Map<String,String> f=form(e);
        String sql="INSERT INTO Feedback(Customer_ID,Feedback_Text,Rating,Feedback_Date) VALUES(?,?,?,CURDATE())";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,Integer.parseInt(f.get("customerId")));p.setString(2,f.get("text"));p.setInt(3,Integer.parseInt(f.get("rating")));p.executeUpdate();
            page(e,"Feedback","Feedback submitted successfully.");
        }catch(SQLException ex){page(e,"Error",ex.getMessage());}
    }

    static Map<String,String> form(HttpExchange e)throws IOException{
        if(!e.getRequestMethod().equalsIgnoreCase("POST"))return new HashMap<>();
        String body=new String(e.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
        Map<String,String> m=new HashMap<>();
        for(String pair:body.split("&")){
            String[] a=pair.split("=",2);
            if(a.length==2)m.put(URLDecoder.decode(a[0],"UTF-8"),URLDecoder.decode(a[1],"UTF-8"));
        }
        return m;
    }

    static void page(HttpExchange e,String title,String body)throws IOException{
        send(e,"<html><head><title>"+esc(title)+"</title><link rel='stylesheet' href='/style.css'></head><body><main class='card'><h1>"+esc(title)+"</h1><div>"+body+"</div><p><a href='/'>Home</a></p></main></body></html>","text/html");
    }
    static String esc(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    static void send(HttpExchange e,String s,String type)throws IOException{
        byte[] b=s.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type",type+"; charset=UTF-8");e.sendResponseHeaders(200,b.length);
        try(OutputStream o=e.getResponseBody()){o.write(b);}
    }

    static class Database {
        static Connection getConnection() throws SQLException { return DriverManager.getConnection(DB_URL,DB_USER,DB_PASSWORD); }
        static void initialize() throws SQLException {
            try(Connection c=getConnection();Statement s=c.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS Customer(Customer_ID INT PRIMARY KEY AUTO_INCREMENT,Customer_Name VARCHAR(100) NOT NULL,Gender VARCHAR(10),Email VARCHAR(100) UNIQUE,Phone_No VARCHAR(15),Address VARCHAR(255),City VARCHAR(50),Username VARCHAR(50) UNIQUE,Password VARCHAR(100) NOT NULL,Registration_Date DATE)");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS TourPackage(Package_ID INT PRIMARY KEY AUTO_INCREMENT,Package_Name VARCHAR(100) NOT NULL,Destination VARCHAR(100),Duration INT,Price DECIMAL(10,2),Description VARCHAR(255))");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS Booking(Booking_ID INT PRIMARY KEY AUTO_INCREMENT,Customer_ID INT,Package_ID INT,Booking_Date DATE,Number_Of_Persons INT,Total_Amount DECIMAL(10,2),Booking_Status VARCHAR(20),FOREIGN KEY(Customer_ID) REFERENCES Customer(Customer_ID),FOREIGN KEY(Package_ID) REFERENCES TourPackage(Package_ID))");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS Feedback(Feedback_ID INT PRIMARY KEY AUTO_INCREMENT,Customer_ID INT,Feedback_Text VARCHAR(255),Rating INT,Feedback_Date DATE,FOREIGN KEY(Customer_ID) REFERENCES Customer(Customer_ID))");
                try(ResultSet r=s.executeQuery("SELECT COUNT(*) FROM TourPackage")){r.next();if(r.getInt(1)==0){
                    s.executeUpdate("INSERT INTO TourPackage(Package_Name,Destination,Duration,Price,Description) VALUES('Goa Beach Tour','Goa',5,15000,'5 Days and 4 Nights'),('Manali Adventure','Manali',6,18000,'6 Days Adventure Tour'),('Rajasthan Heritage','Rajasthan',7,22000,'7 Days Heritage Tour')");
                }}
            }
        }
    }
}