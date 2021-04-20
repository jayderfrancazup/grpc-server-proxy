import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import com.google.protobuf.Message.*;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.text.MessageFormat;

@Slf4j
public final class ProxyMethodHandler<Req, Resp> implements
        ServerCalls.UnaryMethod<Req, Resp>,
        ServerCalls.ServerStreamingMethod<Req, Resp>,
        ServerCalls.ClientStreamingMethod<Req, Resp>,
        ServerCalls.BidiStreamingMethod<Req, Resp> {

    MethodDescriptor<?, ?> descriptor;

    ProxyMethodHandler(MethodDescriptor<?, ?> descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    //@java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, StreamObserver<Resp> responseObserver) {

        // Aqui onde ocorre o processamento da requisicao do metodo

        Message response = null;
        Builder builder = gRPCUtils.getBuilderFromMarshaller(descriptor.getResponseMarshaller());
        ObjectMapper mapper = new ObjectMapper();
        JsonFormat.Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();
        JsonFormat.Parser parser = JsonFormat.parser();

        try {
            String data = printer.print((MessageOrBuilder) request);
            log.info(MessageFormat.format("Processando a requisição do cliente {0} ...", data));
            JsonNode json = mapper.readTree(data);
            JsonNode result = DataUtils.lookupResponse(json);
            data = mapper.writeValueAsString(result);
            log.info(MessageFormat.format("Enviando o resultado para o cliente: {0} ...", data));
            parser.merge(data, builder);
            assert builder != null;
            response = builder.build();
        } catch (IOException ex) {
            log.error("Erro ao processar a requisição", ex);
        }

        responseObserver.onNext((Resp) response);
        responseObserver.onCompleted();
    }

    @Override
    //@java.lang.SuppressWarnings("unchecked")
    public StreamObserver<Req> invoke(StreamObserver<Resp> responseObserver) {
        return null;
    }
}
