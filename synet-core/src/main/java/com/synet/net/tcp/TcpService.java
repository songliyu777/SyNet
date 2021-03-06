package com.synet.net.tcp;

import com.synet.net.protocol.NetProtocol;
import com.synet.net.protocol.ProtocolHeadDefine;
import com.synet.net.service.NetService;
import com.synet.net.session.ISession;
import com.synet.protobuf.protocol.Syprotocol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStoppedEvent;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.ByteBuffer;

@Slf4j
public class TcpService implements ApplicationListener, NetService {

    TcpServiceHandler handler;

    TcpNetServer server;

    public TcpService(TcpNetServer server, TcpServiceHandler handler) throws Exception {
        this.handler = handler;
        this.server = server;
        this.server.setProcessHandler(this::process);
        this.server.setErrorHandler(this::error);
        this.server.doOnConnection(this::connection);
        this.server.createServer();
    }

    public TcpNetServer GetServer() {
        return server;
    }

    void process(NetProtocol protocol) {
        Mono<ByteBuffer> buf = handler.invoke(protocol.getByteBuffer());
        buf.map((b) -> NetProtocol.create(b))
                .subscribe(t -> server.send(t.getHead().getSession(), t.getByteBuffer()), e -> log.error(e.toString()));
    }

    void connection(ISession session) {
        //明文发送sessionid用于加密
        Syprotocol.stc_connect connect = Syprotocol.stc_connect.newBuilder().setSessionId(session.getId()).build();
        NetProtocol protocol = NetProtocol.create(ProtocolHeadDefine.NO_ENCRYPT_PROTOBUF_HEAD,
                ProtocolHeadDefine.VERSION,
                1,
                (short) Syprotocol.protocol_id.connect_msg_VALUE,
                session.getId(),
                connect.toByteArray());
        session.send(protocol.toArray());
    }

    void error(Throwable e) {
        if (e instanceof IOException) {
            return;
        }
        e.printStackTrace();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextStoppedEvent) {
            server.getServer().dispose();
        }
    }

    @Override
    public void send(long id, ByteBuffer buffer) {
        server.send(id, buffer);
    }
}
