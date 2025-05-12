package prg12;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ.Poller;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class lec_05_prg_12_p2p_dechat {
    public static String get_local_ip() {
        java.net.DatagramSocket sock = null;
        try {
            sock = new java.net.DatagramSocket();
            sock.connect(InetAddress.getByName("8.8.8.8"),80);
            return sock.getLocalAddress().getHostAddress();
        } catch (java.net.SocketException | java.net.UnknownHostException e) {
            try {
                String hostname = InetAddress.getLocalHost().getHostName();
                return InetAddress.getByName(hostname).getHostAddress();
            } catch (java.net.UnknownHostException ex) {
                return "127.0.0.1";
            }
        }
        finally {
            if (sock != null) {
                sock.close();
            }
        }
    }
    public static String search_nameserver(String ip_mask, String local_ip_addr, int port_nameserver) {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket req = context.createSocket(SocketType.SUB);
            for (int last = 1; last < 255; last++) {
                String target_ip_addr = String.format("tcp://%s.%d:%d", ip_mask, last, port_nameserver);
                if (!target_ip_addr.equals(local_ip_addr) || target_ip_addr.equals(local_ip_addr)) {
                    req.connect(target_ip_addr);
                }
                req.setReceiveTimeOut(2000);
                req.subscribe("NAMESERVER".getBytes(ZMQ.CHARSET));
            }
            try {
                String res = new String(req.recv(), ZMQ.CHARSET);
                String[] resList = res.split(":");
                if (resList[0].equals("NAMESERVER")) {
                    return resList[1];
                } else {
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
        }
    }
    public static void beacon_nameserver(String local_ip_addr, int port_nameserver) {
        try (ZContext context = new ZContext()) {
            Socket socket = context.createSocket(SocketType.PUB);
            socket.bind(String.format("tcp://%s:%d",local_ip_addr, port_nameserver));
            System.out.println(String.format("local p2p name server bind to tcp://%s:%d.", local_ip_addr, port_nameserver));
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                    String msg = "NAMESERVER:" + local_ip_addr;
                    socket.send(msg, 0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    break;
                }
            }
        }
    }
    public static void user_manager_nameserver(String local_ip_addr, int port_subscribe) {
        List<String[]> user_db = new ArrayList<>();
        try (ZContext context = new ZContext()) {
            Socket socket = context.createSocket(SocketType.REP);
            socket.bind(String.format("tcp://%s:%d", local_ip_addr, port_subscribe));
            System.out.println(String.format("local p2p db server activated at tcp://%s:%d.", local_ip_addr, port_subscribe));
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String[] user_req = new String(socket.recv(), ZMQ.CHARSET).split(":");
                    user_db.add(user_req);
                    System.out.println(String.format("user registration '%s' from '%s'.", user_req[1], user_req[0]));
                    socket.send("ok".getBytes(ZMQ.CHARSET), 0);
                } catch (Exception e) {
                    break;
                }
            }
        }
    }
    public static void relay_server_nameserver(String local_ip_addr, int port_chat_publisher, int port_chat_collector) {
        try (ZContext context = new ZContext()) {
            Socket publisher = context.createSocket(SocketType.PUB);
            publisher.bind(String.format("tcp://%s:%d", local_ip_addr, port_chat_publisher));
            Socket collector = context.createSocket(SocketType.PULL);
            collector.bind(String.format("tcp://%s:%d", local_ip_addr, port_chat_collector));
            System.out.println(String.format("local p2p relay server activated at tcp://%s:%d & %d.",
                    local_ip_addr,
                    port_chat_publisher,
                    port_chat_collector));
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String message = new String(collector.recv(), ZMQ.CHARSET);
                    System.out.println("p2p-relay:<==> " + message);
                    publisher.send(String.format("RELAY:%s", message),0);
                } catch (Exception e) {
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("usage is 'java Prg12_Dechat _user-name_'.");
        }
        else {
            System.out.println("starting p2p chatting program.");
            String ip_addr_p2p_server = "";
            final int port_nameserver = 9005;
            final int port_chat_publisher = 9002;
            final int port_chat_collector = 9003;
            final int port_subscribe = 9004;

            String user_name = args[0];
            String ip_addr = get_local_ip();
            String ip_mask = ip_addr.substring(0, ip_addr.lastIndexOf('.'));

            System.out.println("searching for p2p server.");

            String name_server_ip_addr = search_nameserver(ip_mask, ip_addr, port_nameserver);
            if (name_server_ip_addr == null) {
                ip_addr_p2p_server = ip_addr;
                System.out.println("p2p server is not found, and p2p server mode is activated.");
                Thread beacon_thread = new Thread(() -> {beacon_nameserver(ip_addr, port_nameserver);});
                beacon_thread.start();
                System.out.println("p2p beacon server is activated.");
                Thread db_thread = new Thread(() -> {user_manager_nameserver(ip_addr, port_subscribe);});
                db_thread.start();
                System.out.println("p2p subscriber database server is activated.");
                Thread relay_thread = new Thread(() -> {relay_server_nameserver(ip_addr, port_chat_publisher, port_chat_collector);});
                relay_thread.start();
                System.out.println("p2p message relay server is activated.");
            }
            else {
                ip_addr_p2p_server = name_server_ip_addr;
                System.out.println("p2p server found at " + ip_addr_p2p_server + ", and p2p client mode is activated.");
            }

            System.out.println("starting user registration procedure.");

            try (ZContext db_client_context = new ZContext(); ZContext relay_client = new ZContext()) {
                Socket db_client_socket = db_client_context.createSocket(SocketType.REQ);
                db_client_socket.connect(String.format("tcp://%s:%d", ip_addr_p2p_server, port_subscribe));
                db_client_socket.send(String.format("%s:%s", ip_addr, user_name));
                if (db_client_socket.recvStr().equals("ok")) {
                    System.out.println("user registration to p2p server completed.");
                } else {
                    System.out.println("user registration to p2p server failed.");
                }

                System.out.println("starting message transfer procedure.");

                Socket p2p_rx = relay_client.createSocket(SocketType.SUB);
                p2p_rx.subscribe("RELAY".getBytes(ZMQ.CHARSET));
                p2p_rx.connect(String.format("tcp://%s:%d", ip_addr_p2p_server, port_chat_publisher));
                Socket p2p_tx = relay_client.createSocket(SocketType.PUSH);
                p2p_tx.connect(String.format("tcp://%s:%d", ip_addr_p2p_server, port_chat_collector));

                System.out.println("starting autonomous message transmit and receive scenario.");
                Poller poller = relay_client.createPoller(1);
                poller.register(p2p_rx, Poller.POLLIN);
                Random random = new Random(System.currentTimeMillis());

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        poller.poll(100);
                        if (poller.pollin(0)) {
                            String message = p2p_rx.recvStr();
                            System.out.println(String.format("p2p-recv::<<== %s:%s",
                                    message.split(":")[1],
                                    message.split(":")[2]));
                        }
                        else {
                            int rand = random.nextInt(100) + 1;
                            if (rand < 10) {
                                Thread.sleep(3000);
                                String msg = "(" + user_name + "," + ip_addr + ":ON)";
                                p2p_tx.send(msg);
                                System.out.println("p2p-send::==>> " + msg);
                            }
                            else if (rand > 90) {
                                Thread.sleep(3000);
                                String msg = "(" + user_name + "," + ip_addr + ":OFF)";
                                p2p_tx.send(msg);
                                System.out.println("p2p-send::==>> " + msg);
                            }
                        }
                    } catch (Exception e) {
                        break;
                    }
                }
                System.out.println("closing p2p chatting program.");
            }
        }
    }
}
