package com.ocrsearch.service;

import com.ocrsearch.model.Document;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class DatabaseService {

    private String DB_URL;
    private String DB_USER;
    private String DB_PASS;

    public DatabaseService() {
        try (FileInputStream fis = new FileInputStream("src/main/resources/application.properties")) {
            Properties prop = new Properties();
            prop.load(fis);
            this.DB_URL = prop.getProperty("db.url");
            this.DB_USER = prop.getProperty("db.username");
            this.DB_PASS = prop.getProperty("db.password");
            System.out.println("✅ DatabaseService initialized.");
        } catch (IOException e) {
            System.err.println("❌ ERROR: Could not load application.properties for DB setup. " + e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    /**
     * Inserts a document into the MySQL database.
     * @return The ID of the inserted document, or -1 if insertion failed or document exists.
     */
    public int insertDocument(Document doc) {
        String SQL_INSERT = "INSERT INTO documents(filename, text, upload_time, confidence) VALUES (?, ?, ?, ?)";
        int id = -1;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, doc.getFilename());
            pstmt.setString(2, doc.getText());
            // Use ISO standard format for DATETIME in MySQL
            pstmt.setString(3, doc.getUploadTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pstmt.setDouble(4, doc.getConfidence());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        id = rs.getInt(1);
                        doc.setId(id);
                        System.out.println("✅ DB Insert successful. Document ID: " + id);
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) { // SQLState for Duplicate entry
                System.err.println("⚠️ WARNING: Duplicate document found (Filename: " + doc.getFilename() + "). Skipping DB insert.");
            } else {
                System.err.println("❌ DB Insert Error: " + e.getMessage());
            }
        }
        return id;
    }

    public List<Document> getAllDocuments() {
        List<Document> documents = new ArrayList<>();
        String SQL_SELECT_ALL = "SELECT id, filename, text, upload_time, confidence FROM documents ORDER BY upload_time DESC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_SELECT_ALL)) {

            while (rs.next()) {
                Document doc = new Document();
                doc.setId(rs.getInt("id"));
                doc.setFilename(rs.getString("filename"));
                doc.setText(rs.getString("text"));
                doc.setConfidence(rs.getDouble("confidence"));
                
                // Parse DATETIME
                Timestamp ts = rs.getTimestamp("upload_time");
                doc.setUploadTime(ts.toLocalDateTime());
                
                documents.add(doc);
            }
            System.out.println("✅ Fetched " + documents.size() + " documents from DB.");
        } catch (SQLException e) {
            System.err.println("❌ DB Fetch Error: " + e.getMessage());
        }
        return documents;
    }
}