package com.hissab.client;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * HISSAB Client GUI - A Swing application that connects to the SOAP web service
 * for mathematical expression evaluation and OCR processing.
 */
public class HissabClientGUI extends JFrame {
    
    private static final Logger logger = Logger.getLogger(HissabClientGUI.class.getName());
    
    // GUI Components
    private JTextField expressionField;
    private JTextArea resultArea;
    private JButton calculateButton;
    private JButton ocrButton;
    private JButton clearButton;
    private JLabel imageLabel;
    private JLabel statusLabel;
    private File selectedImageFile;
    private JPanel buttonPanel;
    
    // SOAP Client - will be initialized when web service stubs are generated
    private HissabServiceClient serviceClient;
    
    public HissabClientGUI() {
        initializeGUI();
        initializeServiceClient();
    }
    
    /**
     * Initialize the GUI components
     */
    private void initializeGUI() {
        setTitle("HISSAB - Math Learning Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // Create main panels
        createInputPanel();
        createResultPanel();
        createButtonPanel();
        createStatusPanel();
        
        // Configure window
        pack();
        setLocationRelativeTo(null);
        setResizable(true);
        setMinimumSize(new Dimension(800, 600));
        setPreferredSize(new Dimension(900, 700));
        
        // Set icon (you can add your own icon file)
        try {
            // setIconImage(Toolkit.getDefaultToolkit().getImage("icon.png"));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not load application icon", e);
        }
    }
    
    /**
     * Create the input panel for expression entry and image selection
     */
    private void createInputPanel() {
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(new TitledBorder("Input"));
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Expression input
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        inputPanel.add(new JLabel("Mathematical Expression:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        expressionField = new JTextField(20);
        expressionField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        expressionField.setToolTipText("Enter a mathematical expression (e.g., 2+3*4, (5+3)/2) and press ENTER or click Calculate");
        // Add Enter key listener to trigger calculation
        expressionField.addActionListener(this::calculateExpression);
        inputPanel.add(expressionField, gbc);
        
        // Image selection
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        inputPanel.add(new JLabel("Image File:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        imageLabel = new JLabel("No image selected");
        imageLabel.setForeground(Color.GRAY);
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(this::browseForImage);
        
        imagePanel.add(imageLabel);
        imagePanel.add(browseButton);
        inputPanel.add(imagePanel, gbc);
        
        add(inputPanel, BorderLayout.NORTH);
    }
    
    /**
     * Create the result display panel
     */
    private void createResultPanel() {
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(new TitledBorder("Results"));
        
        resultArea = new JTextArea(15, 50);
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultArea.setBackground(Color.WHITE);
        resultArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        resultPanel.add(scrollPane, BorderLayout.CENTER);
        add(resultPanel, BorderLayout.CENTER);
    }
    
    /**
     * Create the button panel
     */
    private void createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Calculate Expression Button - Primary blue
        calculateButton = new JButton("Calculate Expression");
        calculateButton.setToolTipText("Calculate the mathematical expression");
        calculateButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        calculateButton.setPreferredSize(new Dimension(200, 45));
        calculateButton.setBackground(new Color(0, 123, 255));
        calculateButton.setForeground(Color.WHITE);
        calculateButton.setFocusPainted(false);
        calculateButton.setBorderPainted(false);
        calculateButton.addActionListener(this::calculateExpression);
        
        // Local OCR Button - Orange/Warning color
        ocrButton = new JButton("Process Image (OCR)");
        ocrButton.setToolTipText("Process image with local OCR and calculate");
        ocrButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        ocrButton.setPreferredSize(new Dimension(200, 45));
        ocrButton.setBackground(new Color(255, 165, 0));
        ocrButton.setForeground(Color.WHITE);
        ocrButton.setFocusPainted(false);
        ocrButton.setBorderPainted(false);
        ocrButton.addActionListener(this::processImageLocal);
        
        // Clear Button - Light gray
        clearButton = new JButton("Clear Results");
        clearButton.setToolTipText("Clear the results area");
        clearButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        clearButton.setPreferredSize(new Dimension(130, 35));
        clearButton.setBackground(new Color(108, 117, 125));
        clearButton.setForeground(Color.WHITE);
        clearButton.setFocusPainted(false);
        clearButton.setBorderPainted(false);
        clearButton.addActionListener(this::clearResults);
        
        // Health Check Button - Success green
        JButton healthButton = new JButton("Health Check");
        healthButton.setToolTipText("Check if the web service is running");
        healthButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        healthButton.setPreferredSize(new Dimension(130, 35));
        healthButton.setBackground(new Color(40, 167, 69));
        healthButton.setForeground(Color.WHITE);
        healthButton.setFocusPainted(false);
        healthButton.setBorderPainted(false);
        healthButton.addActionListener(this::performHealthCheck);
        
        buttonPanel.add(calculateButton);
        buttonPanel.add(ocrButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(healthButton);
        
        // We'll add this to the bottom panel in createStatusPanel
        this.buttonPanel = buttonPanel;
    }
    
    /**
     * Create the status panel
     */
    private void createStatusPanel() {
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusLabel.setPreferredSize(new Dimension(0, 25));
        
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        
        // Add status panel above the button panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.PAGE_END);
    }
    
    /**
     * Initialize the SOAP service client
     */
    private void initializeServiceClient() {
        try {
            serviceClient = new HissabServiceClient();
            updateStatus("✓ Connected to HISSAB service - Ready to calculate!");
            appendResult("=== WELCOME TO HISSAB CALCULATOR ===");
            appendResult("✓ Connected to SOAP service successfully");
            appendResult("");
            appendResult("TEXT CALCULATION:");
            appendResult("   • Enter a mathematical expression above and press ENTER");
            appendResult("   • Or click the blue 'Calculate Expression' button");
            appendResult("");
            appendResult("IMAGE CALCULATION:");
            appendResult("   • Select an image file using 'Browse...'");
            appendResult("   • Click 'Process Image (OCR)' to extract and calculate");
            appendResult("");
            appendResult("Local OCR processes images using web-based OCR");
            appendResult("then sends the extracted text to backend for calculation");
            appendResult("=====================================");
            appendResult("");
            logger.log(Level.INFO, "SOAP service client initialized successfully");
        } catch (Exception e) {
            updateStatus("Failed to connect to HISSAB service");
            appendResult("=== CONNECTION ERROR ===");
            appendResult("Failed to connect to HISSAB SOAP service");
            appendResult("Please ensure the service is running at: http://localhost:8085/hissab/HissabService");
            appendResult("========================");
            logger.log(Level.SEVERE, "Failed to initialize SOAP service client", e);
            showErrorMessage("Failed to connect to web service. Please ensure the service is running.");
        }
    }
    
    /**
     * Browse for image file
     */
    private void browseForImage(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Image File");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Image Files", "jpg", "jpeg", "png", "gif", "bmp"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = fileChooser.getSelectedFile();
            imageLabel.setText(selectedImageFile.getName());
            imageLabel.setForeground(Color.BLACK);
            updateStatus("Image selected: " + selectedImageFile.getName());
        }
    }
    
    /**
     * Calculate mathematical expression
     */
    private void calculateExpression(ActionEvent e) {
        String expression = expressionField.getText().trim();
        if (expression.isEmpty()) {
            showErrorMessage("Please enter a mathematical expression");
            expressionField.requestFocus();
            return;
        }
        
        // Disable button during calculation
        calculateButton.setEnabled(false);
        calculateButton.setText("Calculating...");
        updateStatus("Calculating expression...");
        
        // Use SwingWorker for background processing
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return serviceClient.calculateFromString(expression);
            }
            
            @Override
            protected void done() {
                try {
                    String result = get();
                    appendResult("=== CALCULATION RESULT ===");
                    appendResult("Expression: " + expression);
                    appendResult("Result: " + result);
                    appendResult("========================");
                    appendResult("");
                    updateStatus("✓ Calculation completed successfully");
                    logger.log(Level.INFO, "Successfully calculated: " + expression + " = " + result);
                } catch (Exception ex) {
                    String errorMsg = "Error calculating expression: " + ex.getMessage();
                    appendResult("=== ERROR ===");
                    appendResult("Expression: " + expression);
                    appendResult("Error: " + errorMsg);
                    appendResult("=============");
                    appendResult("");
                    updateStatus("❌ Calculation failed");
                    logger.log(Level.SEVERE, "Error calculating expression", ex);
                    showErrorMessage(errorMsg);
                } finally {
                    // Re-enable button
                    calculateButton.setEnabled(true);
                    calculateButton.setText("Calculate Expression");
                }
            }
        };
        worker.execute();
    }
    
    /**
     * Process image using local OCR (equivalent to ENTER for images)
     */
    private void processImageLocal(ActionEvent e) {
        if (selectedImageFile == null) {
            showErrorMessage("Please select an image file first");
            return;
        }
        
        updateStatus("Processing image with local OCR...");
        
        // Use SwingWorker for background processing
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return serviceClient.calculateFromImageWithLocalOCR(selectedImageFile);
            }
            
            @Override
            protected void done() {
                try {
                    String result = get();
                    appendResult("=== LOCAL OCR PROCESSING ===");
                    appendResult("Image: " + selectedImageFile.getName());
                    appendResult("Method: Local OCR → Backend Calculation");
                    appendResult("Result: " + result);
                    appendResult("===========================");
                    appendResult("");
                    updateStatus("✓ Local OCR processing completed successfully");
                    logger.log(Level.INFO, "Successfully processed image with local OCR: " + selectedImageFile.getName());
                } catch (Exception ex) {
                    String errorMsg = "Error in local OCR processing: " + ex.getMessage();
                    appendResult("=== LOCAL OCR ERROR ===");
                    appendResult("Image: " + selectedImageFile.getName());
                    appendResult("Error: " + errorMsg);
                    appendResult("======================");
                    appendResult("");
                    updateStatus("❌ Local OCR processing failed");
                    logger.log(Level.SEVERE, "Error in local OCR processing", ex);
                    showErrorMessage(errorMsg);
                }
            }
        };
        worker.execute();
    }
    
    /**
     * Clear results area
     */
    private void clearResults(ActionEvent e) {
        resultArea.setText("");
        updateStatus("Results cleared");
    }
    
    /**
     * Perform health check
     */
    private void performHealthCheck(ActionEvent e) {
        updateStatus("Checking service health...");
        SwingUtilities.invokeLater(() -> {
            try {
                String result = serviceClient.healthCheck();
                appendResult("Health Check: " + result);
                appendResult("---");
                updateStatus("Health check completed");
            } catch (Exception ex) {
                String errorMsg = "Health check failed: " + ex.getMessage();
                appendResult("Error: " + errorMsg);
                appendResult("---");
                updateStatus("Health check failed");
                showErrorMessage(errorMsg);
            }
        });
    }
    
    /**
     * Append result to the result area
     */
    private void appendResult(String text) {
        SwingUtilities.invokeLater(() -> {
            resultArea.append(text + "\n");
            resultArea.setCaretPosition(resultArea.getDocument().getLength());
        });
    }
    
    /**
     * Update status label
     */
    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }
    
    /**
     * Show error message dialog
     */
    private void showErrorMessage(String message) {
        SwingUtilities.invokeLater(() -> 
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE));
    }
    
    /**
     * Main method to launch the application
     */
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not set system look and feel", e);
        }
        
        // Launch GUI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                new HissabClientGUI().setVisible(true);
                logger.log(Level.INFO, "HISSAB Client GUI started successfully");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to start HISSAB Client GUI", e);
                System.exit(1);
            }
        });
    }
}
