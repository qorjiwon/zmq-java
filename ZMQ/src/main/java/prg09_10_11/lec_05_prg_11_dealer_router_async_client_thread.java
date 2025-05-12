package prg09_10_11;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

public class lec_05_prg_11_dealer_router_async_client_thread {
    private static class ClientTask implements Runnable {
        private final String id;
        private String identity;
        private Socket socket;
        private Poller poller;
        public ClientTask(String id) {
            this.id = id;
        }
        public void recvHandler() {
            while (!Thread.currentThread().isInterrupted()) {
                for (int centitick = 0; centitick < 100; centitick++) {
                    poller.poll(10);
                    if (poller.pollin(0)) {
                        ZMsg msg = ZMsg.recvMsg(socket);
                        System.out.println(identity + " received: " + new String(msg.getLast().getData(), ZMQ.CHARSET));
                        msg.destroy();
                    }
                }
            }
        }

        @Override
        public void run() {
            try (ZContext context = new ZContext()) {
                socket = context.createSocket(SocketType.DEALER);
                identity = id;
                socket.setIdentity(identity.getBytes(ZMQ.CHARSET));
                socket.connect("tcp://localhost:5570");
                System.out.println("client " + identity + " started");
                poller = context.createPoller(1);
                poller.register(socket, Poller.POLLIN);
                int requestNbr = 0;

                Thread clientThread = new Thread(this::recvHandler);
                clientThread.setDaemon(true);
                clientThread.start();

                while (!Thread.currentThread().isInterrupted()) {
                    requestNbr = requestNbr + 1;
                    System.out.println("Req #" + requestNbr + " sent..");
                    socket.send(String.format("request #%d", requestNbr), 0);
                    try {
                        Thread.sleep(1000);
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    public static void main(String[] args) {
        new Thread(new ClientTask(args[0])).start();
    }
}
