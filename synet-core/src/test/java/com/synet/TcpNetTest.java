package com.synet;

import com.synet.net.session.SessionManager;
import com.synet.net.tcp.TcpNetClient;
import com.synet.net.tcp.TcpNetServer;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TcpNetTest {

    @Test
    public void testTcpSend() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(10);

        TcpNetServer server = new TcpNetServer("", 1234, new SessionManager());

        server.setProcessHandler((protocol) -> {
            System.out.println("Success");
            latch.countDown();
        });

        server.doOnConnection(c->System.out.println("OnConnection:" + c));
        server.doOnDisconnection(c->System.out.println("OnDisconnection:" + c));

        server.createServer();

        byte a[] = {(byte) 0xff, (byte) 0xff, 0x00, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x01, (byte) 0xff, (byte) 0xfe, (byte) 0xe7, (byte) 0x04, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0x10};

        TcpNetClient client = new TcpNetClient("192.168.127.126", 8888);

        client.connectServer();

        for (int i = 0; i < 1; i++) {
            Mono<Integer> m = Mono.just(i);
            m.delaySubscription(Duration.ofMillis(i * 10))
                    .doOnSuccess((t) -> client.send(a))
                    .block();
        }

        Assert.assertTrue("finished", latch.await(5, TimeUnit.SECONDS));
    }
}
