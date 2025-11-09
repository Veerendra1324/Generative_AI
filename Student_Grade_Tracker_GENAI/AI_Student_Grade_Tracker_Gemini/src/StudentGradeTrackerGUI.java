import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import javax.swing.*;

public class StudentGradeTrackerGUI extends JFrame {
    private JTextField nameField;
    private JTextField subjectsField;
    private JTextField marksField;
    private JTextArea outputArea;
    private JLabel averageLabel;
    private GeminiClient gemini;

    public StudentGradeTrackerGUI() {
        super("AI-Powered Student Grade Tracker (Gemini)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Menu for API Key
        JMenuBar menuBar = new JMenuBar();
        JMenu settings = new JMenu("Settings");
        JMenuItem setKey = new JMenuItem("Set Gemini API Key…");
        setKey.addActionListener(e -> {
            String key = JOptionPane.showInputDialog(this, "Enter Gemini API Key:", "");
            if (key != null && !key.isBlank()) {
                gemini.setApiKey(key.trim());
                JOptionPane.showMessageDialog(this, "API key set.");
            }
        });
        settings.add(setKey);
        menuBar.add(settings);
        setJMenuBar(menuBar);

        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel("Student Name:"));
        nameField = new JTextField(20);
        row1.add(nameField);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(new JLabel("Subjects (comma-separated):"));
        subjectsField = new JTextField(30);
        row2.add(subjectsField);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row3.add(new JLabel("Marks (comma-separated):"));
        marksField = new JTextField(30);
        row3.add(marksField);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton calcBtn = new JButton("Calculate");
        JButton aiBtn = new JButton("Feedback");
        buttonRow.add(calcBtn);
        buttonRow.add(aiBtn);

        form.add(row1);
        form.add(row2);
        form.add(row3);
        form.add(buttonRow);

        outputArea = new JTextArea(14, 60);
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        averageLabel = new JLabel("Average: -");

        JPanel center = new JPanel(new BorderLayout());
        center.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        center.add(averageLabel, BorderLayout.SOUTH);

        add(form, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        // Initialize Gemini client (will attempt env var if no key passed here)
        gemini = new GeminiClient(null);

        calcBtn.addActionListener(this::onCalculate);
        aiBtn.addActionListener(this::onGenerateAI);
    }

    private void onCalculate(ActionEvent e) {
        try {
            String[] subjects = splitCSV(subjectsField.getText());
            double[] marks = parseMarks(splitCSV(marksField.getText()));
            if (subjects.length != marks.length) {
                JOptionPane.showMessageDialog(this, "Subjects and marks count must match.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Student s = new Student(nameField.getText().trim(), subjects, marks);
            averageLabel.setText(String.format("Average: %.2f", s.average()));

            StringBuilder sb = new StringBuilder();
            sb.append("Details:\n");
            for (int i = 0; i < subjects.length; i++) {
                sb.append(" - ").append(subjects[i]).append(": ").append(marks[i]).append("\n");
            }
            sb.append(String.format("\nTotal: %.2f\nAverage: %.2f\n", s.total(), s.average()));
            outputArea.setText(sb.toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onGenerateAI(ActionEvent e) {
        try {
            String name = nameField.getText().trim();
            String[] subjects = splitCSV(subjectsField.getText());
            double[] marks = parseMarks(splitCSV(marksField.getText()));
            if (name.isBlank()) {
                JOptionPane.showMessageDialog(this, "Please enter the student's name.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (subjects.length == 0 || marks.length == 0 || subjects.length != marks.length) {
                JOptionPane.showMessageDialog(this, "Please provide matching subjects and marks.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Student s = new Student(name, subjects, marks);
            double average = s.average();

            StringBuilder table = new StringBuilder();
            for (int i = 0; i < subjects.length; i++) {
                table.append(subjects[i]).append(": ").append(marks[i]).append(i < subjects.length - 1 ? ", " : "");
            }

            StringBuilder prompt = new StringBuilder();
            prompt.append("You are a helpful academic mentor. Analyze the student's performance and write concise feedback (120-180 words). Include:\n");
            prompt.append("- Top 2 strengths and the subjects they relate to\n");
            prompt.append("- 2-3 specific, actionable suggestions for improvement\n");
            prompt.append("- A short motivational closing line\n\n");
            prompt.append("Student:\n");
            prompt.append("- Name: ").append(name).append("\n");
            prompt.append("- Subjects & Marks: ").append(table.toString()).append("\n");
            prompt.append(String.format("- Average: %.2f\n\n", average));
            prompt.append("Keep the tone supportive and professional. Avoid markdown.");

            outputArea.setText("Contacting Gemini for feedback... Please wait.");
            // Run network call off the EDT
            new Thread(() -> {
                try {
                    String ai = gemini.generateFeedback(prompt.toString());
                    SwingUtilities.invokeLater(() -> outputArea.setText("AI Feedback:\n\n" + ai));
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> outputArea.setText("AI Error: " + ex.getMessage()));
                }
            }).start();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String[] splitCSV(String s) {
        if (s == null || s.trim().isEmpty()) return new String[0];
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .toArray(String[]::new);
    }

    private static double[] parseMarks(String[] arr) {
        double[] out = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            out[i] = Double.parseDouble(arr[i]);
        }
        return out;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentGradeTrackerGUI().setVisible(true));
    }
}
