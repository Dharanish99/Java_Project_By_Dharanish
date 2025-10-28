package com.ocrsearch;

import com.ocrsearch.model.Document;
import com.ocrsearch.service.DatabaseService;
import com.ocrsearch.service.OCRService;
import com.ocrsearch.service.SearchService;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {

    private final OCRService ocrService;
    private final DatabaseService dbService;
    private final SearchService searchService;
    private final Scanner scanner;

    public Main() {
        System.out.println("Initializing Intelligent OCR & Document Search System...");
        this.ocrService = new OCRService();
        this.dbService = new DatabaseService();
        this.searchService = new SearchService();
        this.scanner = new Scanner(System.in);
        System.out.println("Initialization complete. Starting application.");
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.run();
    }

    public void run() {
        int choice;
        do {
            displayMenu();
            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                scanner.nextLine(); // consume newline
                processChoice(choice);
            } else {
                System.out.println("🚫 Invalid input. Please enter a number.");
                scanner.nextLine(); // consume invalid input
                choice = 0; // reset choice
            }
        } while (choice != 4);
        System.out.println("\n👋 Exiting system. Goodbye!");
    }

    private void displayMenu() {
        System.out.println("\n=================================================");
        System.out.println("  Intelligent OCR & Document Search System (CLI) ");
        System.out.println("=================================================");
        System.out.println("1. Upload & Process Document (OCR, DB, ES)");
        System.out.println("2. View All Stored Documents (DB)");
        System.out.println("3. Search Documents by Keyword (Elasticsearch)");
        System.out.println("4. Exit");
        System.out.print("Enter your choice: ");
    }

    private void processChoice(int choice) {
        switch (choice) {
            case 1:
                uploadAndProcessDocument();
                break;
            case 2:
                viewAllDocuments();
                break;
            case 3:
                searchDocuments();
                break;
            case 4:
                // Handled in run() loop
                break;
            default:
                System.out.println("🚫 Invalid choice. Please select a valid option (1-4).");
        }
    }

    private void uploadAndProcessDocument() {
        System.out.print("Enter full path to image/document (JPG, PNG, PDF): ");
        String filePath = scanner.nextLine().trim();

        try {
            // 1. OCR Processing
            String[] ocrResult = ocrService.performOCR(filePath);
            String extractedText = ocrResult[0];
            double confidence = Double.parseDouble(ocrResult[1]);

            if (extractedText.trim().isEmpty()) {
                System.out.println("⚠️ OCR produced no readable text. Aborting process.");
                return;
            }

            File file = new File(filePath);
            Document doc = new Document(file.getName(), extractedText, confidence);
            
            System.out.println("\n--- Extracted OCR Text ---");
            System.out.println(extractedText.substring(0, Math.min(extractedText.length(), 500)) + "...");
            System.out.printf("Confidence Score: %.2f%%\n", confidence);
            System.out.println("--------------------------");

            // 2. Database Storage
            int dbId = dbService.insertDocument(doc);

            // 3. Elasticsearch Indexing
            if (dbId != -1) {
                searchService.indexDocument(doc, dbId);
            }

        } catch (IllegalArgumentException e) {
            System.err.println("❌ FILE ERROR: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ An unexpected error occurred during processing: " + e.getMessage());
        }
    }

    private void viewAllDocuments() {
        System.out.println("\n--- Stored Documents (MySQL) ---");
        List<Document> documents = dbService.getAllDocuments();
        if (documents.isEmpty()) {
            System.out.println("No documents currently stored.");
        } else {
            documents.forEach(System.out::println);
        }
        System.out.println("----------------------------------");
    }

    private void searchDocuments() {
        System.out.print("Enter keyword for full-text search: ");
        String keyword = scanner.nextLine().trim();
        if (!keyword.isEmpty()) {
            searchService.searchDocuments(keyword);
        } else {
            System.out.println("🚫 Keyword cannot be empty.");
        }
    }
}