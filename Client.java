import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class QuizClientGUI {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 1234;
    private JFrame frame;
    private JTextArea questionArea;
    private JTextField answerField;
    private JTextArea feedbackArea;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;

    public QuizClientGUI() {
        setupGUI();
    }

    private void setupGUI() {
        frame = new JFrame("Quiz Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        questionArea = new JTextArea(5, 30);
        questionArea.setEditable(false);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);

        feedbackArea = new JTextArea(5, 30);
        feedbackArea.setEditable(false);
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);

        answerField = new JTextField(20);
        JButton submitButton = new JButton("Submit");

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Your Answer: "));
        inputPanel.add(answerField);
        inputPanel.add(submitButton);

        frame.add(new JScrollPane(questionArea), BorderLayout.NORTH);
        frame.add(inputPanel, BorderLayout.CENTER);
        frame.add(new JScrollPane(feedbackArea), BorderLayout.SOUTH);

        submitButton.addActionListener(e -> sendAnswer());

        frame.setVisible(true);

        // 리소스 종료
        Runtime.getRuntime().addShutdownHook(new Thread(this::closeResources));
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        if (serverMessage.startsWith("QUESTION:")) {
                            questionArea.setText(serverMessage.substring(9));
                        } else if (serverMessage.startsWith("RESULT:")) {
                            feedbackArea.setText(serverMessage.substring(7));
                        } else if (serverMessage.startsWith("SCORE:")) {
                            feedbackArea.append("\nCurrent Score: " + serverMessage.substring(6));
                        } else if (serverMessage.startsWith("FINAL_SCORE:")) {
                            feedbackArea.append("\nFinal Score: " + serverMessage.substring(12));
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Unable to connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
            closeResources();
            System.exit(1);
        }
    }

    private void sendAnswer() {
        String answer = answerField.getText();
        if (!answer.isEmpty() && out != null) {
            out.println("ANSWER:" + answer);
            answerField.setText("");
        }
    }

    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("Resources closed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        QuizClientGUI client = new QuizClientGUI();
        client.connectToServer();
    }
}
