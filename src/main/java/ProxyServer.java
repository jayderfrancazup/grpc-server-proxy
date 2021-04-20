import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProxyServer {

    int port;
    Server server;

    ProxyServer(int port) {
        this.port = port;
        this.server = ServerBuilder
                .forPort(port)
                .fallbackHandlerRegistry(new ProxyHandlerRegistry())
                .build();
    }

    public void start() throws IOException {
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("*** encerrando o servidor gRPC ...");
            try {
                ProxyServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            log.info("*** servidor gRPC encerrado com sucesso");
        }));
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        log.info("Iniciando servidor gRPC ..");
        ProxyServer server = new ProxyServer(10001);
        server.start();
        log.info("Servidor gRPC iniciado e aguardando requisição na porta 10001");
        server.blockUntilShutdown();
    }
}
