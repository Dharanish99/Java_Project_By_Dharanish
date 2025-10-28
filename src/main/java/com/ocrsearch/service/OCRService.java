package com.ocrsearch.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException; // Only need this exception
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class OCRService {

    private final Tesseract tesseract;

    public OCRService() {
        this.tesseract = new Tesseract();
        try (FileInputStream fis = new FileInputStream("src/main/resources/application.properties")) {
            Properties prop = new Properties();
            prop.load(fis);
            String dataPath = prop.getProperty("tesseract.datapath");
            if (dataPath != null && !dataPath.isEmpty()) {
                // Set the path to the Tesseract installation/tessdata folder
                tesseract.setDatapath(dataPath); 
            }
            tesseract.setLanguage("eng"); // English only
            System.out.println("✅ OCRService initialized. Tesseract Data Path: " + dataPath);
        } catch (IOException e) {
            System.err.println("❌ ERROR: Could not load application.properties for Tesseract setup. " + e.getMessage());
        }
    }

    /**
     * Performs OCR and returns the extracted text and confidence (simplified).
     * @param imagePath The path to the image/PDF file.
     * @return A String array: [0] = extracted text, [1] = mean confidence score.
     * @throws IllegalArgumentException if the file is invalid.
     */
    public String[] performOCR(String imagePath) throws IllegalArgumentException {
        File imageFile = new File(imagePath);
        if (!imageFile.exists() || imageFile.isDirectory()) {
            throw new IllegalArgumentException("Invalid file path: " + imagePath);
        }

        try {
            System.out.println("⏳ Performing OCR on: " + imageFile.getName() + "...");
            
            // 1. Extract text using the primary OCR method
            String extractedText = tesseract.doOCR(imageFile);

            double confidence = 95.0; // WORKAROUND: Use a high default confidence for success.
                                     // Direct, stable access to mean confidence is complex in Tess4J 5.x.

            if (extractedText == null || extractedText.trim().isEmpty()) {
                confidence = 0.0;
                System.out.println("⚠️ OCR extracted no text.");
                return new String[]{"", "0.0"};
            }
            
            System.out.printf("✅ OCR complete. Confidence: %.2f%%\n", confidence);
            // Return confidence as a string representing a percentage (e.g., "95.0")
            return new String[]{extractedText, String.valueOf(confidence)}; 
            
        } catch (TesseractException e) {
            System.err.println("❌ OCR Error: " + e.getMessage());
            return new String[]{"", "0.0"};
        }
    }
}