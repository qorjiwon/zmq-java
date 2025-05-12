package prg09_10_11;

import org.zeromq.*;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

public class lec_05_prg_09_dealer_router_async_server {
    private static class server_task implements Runnable
    {
        private int num_server;
        public server_task(int num_server) { this.num_server = num_server; }
        @Override
        public void run()
        {
            try (ZContext ctx = new ZContext()) {
                Socket frontend = ctx.createSocket(SocketType.ROUTER);
                frontend.bind("tcp://*:5570");

                Socket backend = ctx.createSocket(SocketType.DEALER);
                backend.bind("inproc://backend");

                Thread[] workers =  new Thread[num_server];
                for (int threadNbr = 0; threadNbr < num_server; threadNbr++) {
                    Thread worker = new Thread(new server_worker(ctx, threadNbr));
                    worker.start();
                    workers[threadNbr] = worker;
                }
                ZMQ.proxy(frontend, backend, null);
            }
        }
    }

    private static class server_worker implements Runnable
    {
        private ZContext ctx;
        private int id;
        public server_worker(ZContext ctx, int id)
        {
            this.ctx = ctx;
            this.id = id;
        }

        @Override
        public void run()
        {
            Socket worker = ctx.createSocket(SocketType.DEALER);
            worker.connect("inproc://backend");
            System.out.printf("Worker#%d started\n", id);
            while (!Thread.currentThread().isInterrupted()) {
                ZMsg msg = ZMsg.recvMsg(worker);
                ZFrame address = msg.pop();
                ZFrame content = msg.pop();
                System.out.printf("Worker#%d received %s from %s\n", id, new String(content.getData(), ZMQ.CHARSET), new String(address.getData(), ZMQ.CHARSET));
                address.send(worker, ZFrame.REUSE + ZFrame.MORE);
                content.send(worker, ZFrame.REUSE);
                msg.destroy();
                address.destroy();
                content.destroy();
            }
            worker.close();
        }
    }

    public static void main(String[] args) throws Exception
    {
        Thread server = new Thread(new server_task(Integer.parseInt(args[0])));
        server.start();
        server.join();
    }
}
