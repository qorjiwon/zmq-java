package prg01_02;

//import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

public class lec_05_prg_02_req_rep_basic_client {
    public static void main(String[] args)
    {
        try (ZContext context = new ZContext()) {
            System.out.println("Connecting to hello world server...");

            ZMQ.Socket socket = context.createSocket(org.zeromq.SocketType.REQ);
            socket.connect("tcp://localhost:1207");

            for (int requestNbr = 0; requestNbr != 10; requestNbr++) {
                String request = "Hello";
                System.out.println("Sending request " + requestNbr + " ...");
                socket.send(request.getBytes(ZMQ.CHARSET), 0);

                byte[] reply = socket.recv(0);
                System.out.println(
                        "Received reply " + requestNbr + ' ' + new String(reply, ZMQ.CHARSET) + " "
                );
            }
        }
    }
}
