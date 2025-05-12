package prg09_10_11;

import org.zeromq.SocketType;
import org.zeromq.*;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZContext;


public class lec_05_prg_10_dealer_router_async_client {
    private static class client_task implements Runnable
    {
        private String id;
        private client_task(String id) { this.id = id; }

        @Override
        public void run()
        {
            try (ZContext ctx = new ZContext()) {
                Socket client = ctx.createSocket(SocketType.DEALER);
                client.setIdentity(id.getBytes(ZMQ.CHARSET));
                client.connect("tcp://localhost:5570");
                System.out.println("Client " + id + " started");

                Poller poller = ctx.createPoller(1);
                poller.register(client, Poller.POLLIN);

                int requestNbr = 0;
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println("Req #" + requestNbr + " sent..");
                    client.send(String.format("request #%d", ++requestNbr), 0);

                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (int centitick = 0; centitick < 100; centitick++) {
                        poller.poll(10);
                        if (poller.pollin(0)) {
                            ZMsg msg = ZMsg.recvMsg(client);
                            System.out.println(id + " received: " + new String(msg.getLast().getData(), ZMQ.CHARSET));
                            msg.destroy();
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception
    {
        new Thread(new client_task(args[0])).start();
    }
}
