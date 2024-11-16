import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MultiClientQuizServer {
    // 서버가 사용할 포트 번호
    private static final int PORT = 1234;

    // 퀴즈 질문과 정답 리스트
    private static final List<String[]> QUESTIONS = Arrays.asList(
        new String[]{"What is the capital of Korea?", "Seoul"},
        new String[]{"What is 1 + 1?", "2"},
        new String[]{"What subject are you currently taking?", "Computer network"}
    );

    public static void main(String[] args) {
        // Thread Pool 생성: 최대 10개의 클라이언트를 동시에 처리
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        System.out.println("Quiz Server is running...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                // 클라이언트 연결 요청 대기
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected!");
                // 클라이언트 요청을 처리하기 위해 Thread Pool에 작업 할당
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 서버 종료 시 Thread Pool 해제
            threadPool.shutdown();
        }
    }

    // 클라이언트 요청을 처리하는 클래스
    static class ClientHandler implements Runnable {
        private final Socket socket;

        // 클라이언트 소켓을 받아 초기화
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                // 클라이언트와 데이터 송수신을 위한 스트림 생성
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                int score = 0; // 클라이언트 점수 초기화

                // 모든 질문을 클라이언트에 전송
                for (String[] question : QUESTIONS) {
                    out.println("QUESTION:" + question[0]); // 질문 전송
                    String response = in.readLine(); // 클라이언트 응답 대기

                    if (response != null && response.startsWith("ANSWER:")) {
                        // 정답 평가 및 피드백 전송
                        String answer = response.substring(7).trim();
                        if (answer.equalsIgnoreCase(question[1])) {
                            score++;
                            out.println("RESULT:Correct!"); // 정답 메시지
                        } else {
                            out.println("RESULT:Incorrect!"); // 오답 메시지
                        }
                        out.println("SCORE:" + score); // 현재 점수 전송
                    }
                }

                // 최종 점수 전송 후 연결 종료
                out.println("FINAL_SCORE:" + score);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close(); // 소켓 close
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
