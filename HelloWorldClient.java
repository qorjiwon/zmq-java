import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class HelloWorldClient {
    public static void main(String[] args) {
        try (ZContext context = new ZContext()) {
            // 서버에 연결할 소켓 생성
            System.out.println("Hello World 서버에 연결 중...");
            ZMQ.Socket socket = context.createSocket(SocketType.REQ);
            socket.connect("tcp://localhost:5555");

            // 응답을 기다리며 10번의 요청 수행
            for (int request = 0; request < 10; request++) {
                System.out.println("요청 " + request + " 전송 중...");
                socket.send("Hello".getBytes(ZMQ.CHARSET), 0);

                // 응답 받기
                byte[] message = socket.recv(0);
                System.out.println("응답 " + request + " 수신 [ " + new String(message, ZMQ.CHARSET) + " ]");
            }
        }
    }
}

