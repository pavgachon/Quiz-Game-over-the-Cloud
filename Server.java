import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MultiClientQuizServer {
    private static final int PORT = 1234;
    private static final List<String[]> QUESTIONS = Arrays.asList(
        new String[]{"What is the capital of Korea?", "Seoul"},
        new String[]{"What is 1 + 1?", "2"},
        new String[]{"What subject are you currently taking?", "Computer network"}
    );

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(10); // 클라이언트 최대 10개로 제한
        System.out.println("Quiz Server is running...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected!");
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                int score = 0;
                for (String[] question : QUESTIONS) {
                    out.println("QUESTION:" + question[0]);
                    String response = in.readLine();
                    if (response != null && response.startsWith("ANSWER:")) {
                        String answer = response.substring(7).trim();
                        if (answer.equalsIgnoreCase(question[1])) {
                            score++;
                            out.println("RESULT:Correct!");
                        } else {
                            out.println("RESULT:Incorrect!");
                        }
                        out.println("SCORE:" + score);
                    }
                }
                out.println("FINAL_SCORE:" + score);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}


