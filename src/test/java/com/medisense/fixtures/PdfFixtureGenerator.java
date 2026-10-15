package com.medisense.fixtures;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Utility to generate PDF test fixtures for MediSense ingestion and parser test suites.
 */
public class PdfFixtureGenerator {

    public static void main(String[] args) throws Exception {
        String targetDir = "src/test/resources/fixtures/pdfs";
        if (args.length > 0) {
            targetDir = args[0];
        }
        generateAllFixtures(new File(targetDir));
    }

    public static void generateAllFixtures(File outputDirectory) throws IOException {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        generateValidLabReport(new File(outputDirectory, "valid_lab_report.pdf"));
        generateInvalidImageOnlyPdf(new File(outputDirectory, "invalid_lab_report_image_only.pdf"));
        generateLargeLabReport(new File(outputDirectory, "lab_report_large.pdf"));
        generateTypedPrescription(new File(outputDirectory, "prescription_typed.pdf"));
        generateHandwrittenPrescription(new File(outputDirectory, "prescription_handwritten.pdf"));
        generateFormularyTable(new File(outputDirectory, "formulary_table.pdf"));
        generateCorruptedPdf(new File(outputDirectory, "corrupted.pdf"));
    }

    public static void generateValidLabReport(File targetFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.newLineAtOffset(50, 750);
                cs.showText("MEDISENSE CLINICAL LABORATORY REPORT");
                cs.newLineAtOffset(0, -25);

                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.showText("Patient Name: Ramesh Patel  |  Age: 52  |  Gender: Male  |  Date: 2026-10-14");
                cs.newLineAtOffset(0, -20);
                cs.showText("Ordering Physician: Dr. Sundaram  |  Ref ID: LAB-2026-99214");
                cs.newLineAtOffset(0, -30);

                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.showText("TEST NAME                 RESULT         UNITS       REFERENCE RANGE    STATUS");
                cs.newLineAtOffset(0, -18);

                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.showText("Fasting Blood Glucose     110            mg/dL       70 - 99            BORDERLINE");
                cs.newLineAtOffset(0, -16);
                cs.showText("HbA1c (Glycated Hb)       7.2            %           < 5.7              ELEVATED");
                cs.newLineAtOffset(0, -16);
                cs.showText("Serum Cholesterol         215            mg/dL       < 200              ELEVATED");
                cs.newLineAtOffset(0, -16);
                cs.showText("LDL Cholesterol           145            mg/dL       < 100              BORDERLINE");
                cs.newLineAtOffset(0, -16);
                cs.showText("HDL Cholesterol           42             mg/dL       > 40               NORMAL");
                cs.newLineAtOffset(0, -16);
                cs.showText("Triglycerides             160            mg/dL       < 150              BORDERLINE");
                cs.newLineAtOffset(0, -16);
                cs.showText("Serum Creatinine          0.9            mg/dL       0.7 - 1.2          NORMAL");
                cs.newLineAtOffset(0, -16);
                cs.showText("Blood Urea Nitrogen       14             mg/dL       7 - 20             NORMAL");
                cs.newLineAtOffset(0, -30);

                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
                cs.showText("Note: Borderline lipid parameters suggest metabolic evaluation. Consult your physician.");
                cs.endText();
            }
            document.save(targetFile);
        }
    }

    public static void generateMultiPageLabReport(File targetFile, int pages) throws IOException {
        String[] sections = {
                "SECTION 1: COMPLETE BLOOD COUNT & HEMATOLOGY",
                "SECTION 2: COMPREHENSIVE METABOLIC & LIPID PROFILE",
                "SECTION 3: HEPATIC FUNCTION & ENZYME PANEL",
                "SECTION 4: RENAL FUNCTION & ELECTROLYTE EVALUATION",
                "SECTION 5: ENDOCRINE, THYROID & HbA1c BIOMARKERS"
        };

        try (PDDocument document = new PDDocument()) {
            for (int p = 0; p < pages; p++) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    cs.newLineAtOffset(50, 750);
                    cs.showText("MEDISENSE CLINICAL LABORATORY REPORT - PAGE " + (p + 1) + " OF " + pages);
                    cs.newLineAtOffset(0, -25);

                    String sectionTitle = (p < sections.length) ? sections[p] : ("SECTION " + (p + 1) + ": CLINICAL OBSERVATIONS");
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                    cs.showText(sectionTitle);
                    cs.newLineAtOffset(0, -20);

                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.showText("Diagnostic observations recorded for patient ID MED-99214. Fasting state confirmed.");
                    cs.newLineAtOffset(0, -18);
                    cs.showText("Parameter values verified within standard clinical deviations for physiological profiling.");
                    cs.endText();
                }
            }
            document.save(targetFile);
        }
    }

    public static void generate100PagePdf(File targetFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (int i = 1; i <= 100; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(50, 700);
                    cs.showText("MediSense Clinical Archive Diagnostic Record Page " + i + " of 100. Patient ID: PT-" + (1000 + i));
                    cs.newLineAtOffset(0, -20);
                    cs.showText("Serum biomarkers and routine screening results recorded in compliance with medical data regulations.");
                    cs.endText();
                }
            }
            document.save(targetFile);
        }
    }

    public static void generateInvalidImageOnlyPdf(File targetFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            // Draw a decorative box with no text characters to simulate pure image / scanned bitmap without OCR
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.addRect(100, 100, 400, 600);
                cs.stroke();
            }
            document.save(targetFile);
        }
    }

    public static void generateLargeLabReport(File targetFile) throws IOException {
        // Generates a valid PDF with file size > 10MB to test upload size constraints
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < 50; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(50, 700);
                    cs.showText("Large Clinical Dataset Diagnostic Archive Page " + (i + 1));
                    cs.endText();
                }
            }
            document.save(targetFile);
        }

        // Pad file with PDF comment data to exceed 10.5 MB
        try (FileOutputStream fos = new FileOutputStream(targetFile, true)) {
            byte[] padding = new byte[1024 * 1024]; // 1MB buffer
            for (int i = 0; i < padding.length; i++) {
                padding[i] = (byte) ('A' + (i % 26));
            }
            // Write 11 chunks = 11 MB
            for (int k = 0; k < 11; k++) {
                fos.write("\n% PADDING_DATA_FOR_FILE_SIZE_LIMIT_TESTING: ".getBytes(StandardCharsets.US_ASCII));
                fos.write(padding);
            }
        }
    }

    public static void generateTypedPrescription(File targetFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.newLineAtOffset(50, 750);
                cs.showText("APOLLO CLINIC - MEDICAL PRESCRIPTION");
                cs.newLineAtOffset(0, -25);

                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.showText("Doctor: Dr. Sunita Rao, MD (Internal Medicine)  |  Reg No: KMC-44812");
                cs.newLineAtOffset(0, -18);
                cs.showText("Patient: Rajesh Sharma  |  Age: 48  |  Date: 2026-10-14");
                cs.newLineAtOffset(0, -30);

                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.showText("Rx (Medications Prescribed):");
                cs.newLineAtOffset(0, -20);

                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.showText("1. Tab Glycomet 500 SR (Metformin 500mg) - 1 tab orally once daily after dinner x 30 days");
                cs.newLineAtOffset(0, -18);
                cs.showText("2. Tab Atorva 10 (Atorvastatin 10mg) - 1 tab orally at bedtime x 30 days");
                cs.newLineAtOffset(0, -18);
                cs.showText("3. Tab Telma 40 (Telmisartan 40mg) - 1 tab orally in morning after breakfast x 30 days");
                cs.newLineAtOffset(0, -30);

                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
                cs.showText("Instructions: Low salt diet, regular morning walk 30 mins. Review in 1 month with lipid profile.");
                cs.endText();
            }
            document.save(targetFile);
        }
    }

    public static void generateHandwrittenPrescription(File targetFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.TIMES_ITALIC, 11);
                cs.newLineAtOffset(50, 730);
                cs.showText("Dr. V. Ramanathan Clinic - OP Note");
                cs.newLineAtOffset(0, -22);
                cs.showText("Pt: Meena, 55 F - c/o fatigue, high BP noted 150/90");
                cs.newLineAtOffset(0, -22);
                cs.showText("Rx: Amlodipine 5mg OD, Metformin 500 BD");
                cs.newLineAtOffset(0, -22);
                cs.showText("Adv: FBS, PPBS, S. Creat, ECG");
                cs.endText();

                // Add simulated lines/borders
                cs.setLineWidth(0.5f);
                cs.moveTo(50, 720);
                cs.lineTo(400, 720);
                cs.stroke();
            }
            document.save(targetFile);
        }
    }

    public static void generateFormularyTable(File targetFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
                cs.newLineAtOffset(50, 750);
                cs.showText("NATIONAL ESSENTIAL MEDICINES FORMULARY TABLE");
                cs.newLineAtOffset(0, -25);

                cs.setFont(PDType1Font.COURIER_BOLD, 9);
                cs.showText("+----------------------+---------------------+-------------------+------------+");
                cs.newLineAtOffset(0, -14);
                cs.showText("| Generic Name         | Drug Class          | Standard Dosage   | Route      |");
                cs.newLineAtOffset(0, -14);
                cs.showText("+----------------------+---------------------+-------------------+------------+");
                cs.newLineAtOffset(0, -14);

                cs.setFont(PDType1Font.COURIER, 9);
                cs.showText("| Metformin            | Biguanide           | 500mg - 2000mg/d  | Oral       |");
                cs.newLineAtOffset(0, -14);
                cs.showText("| Atorvastatin         | Statin              | 10mg - 80mg daily | Oral       |");
                cs.newLineAtOffset(0, -14);
                cs.showText("| Amlodipine           | DHP-CCB             | 2.5mg - 10mg/d    | Oral       |");
                cs.newLineAtOffset(0, -14);
                cs.showText("| Losartan             | ARB                 | 25mg - 100mg/d    | Oral       |");
                cs.newLineAtOffset(0, -14);
                cs.showText("| Paracetamol          | Analgesic           | 500mg - 1000mg q6h| Oral       |");
                cs.newLineAtOffset(0, -14);
                cs.showText("+----------------------+---------------------+-------------------+------------+");
                cs.endText();
            }
            document.save(targetFile);
        }
    }

    public static void generateCorruptedPdf(File targetFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write("%PDF-1.4 CORRUPTED FILE HEADER".getBytes(StandardCharsets.UTF_8));
            fos.write(new byte[]{0x00, 0x12, (byte) 0xFF, (byte) 0xFE, 0x42, 0x77});
            fos.write("%%EOF INCOMPLETE TAIL WITHOUT XREF".getBytes(StandardCharsets.UTF_8));
        }
    }
}
