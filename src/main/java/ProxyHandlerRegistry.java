import io.grpc.HandlerRegistry;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCallHandler;
import io.grpc.ServerMethodDefinition;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.text.MessageFormat;

@Slf4j
public class ProxyHandlerRegistry extends HandlerRegistry {

    @Nullable
    @Override
    public ServerMethodDefinition<?, ?> lookupMethod(String s, @Nullable String s1) {

        ServerMethodDefinition<?, ?> definition = null;
        MethodDescriptor<?, ?> descriptor = null;

        MethodDescriptor<Object, Object> fake = null;
        ServerCallHandler<Object, Object> call = null;

        String serviceName = s.split("/")[0];
        String methodName = s.split("/")[1];

        log.info(MessageFormat.format("Procurando pelo método {1} no serviço ''{0}'' ...", serviceName, methodName));
        descriptor = gRPCUtils.findMethodDescriptor(serviceName, methodName);

        if (descriptor != null) {
            log.info(MessageFormat.format("Criando versão gennérica do método ''{0}'' ...", methodName));
            fake = gRPCUtils.makeFakeMethod(descriptor);

            log.info(MessageFormat.format("Determinando o tipo de chamada método ''{0}'' ...", methodName));
            call = gRPCUtils.getServerCallHandlerByType(descriptor);
        }

        if (fake != null && call != null)
            log.info(MessageFormat.format("Criando a definição do serviço ''{0}'' ...", s));
            definition = ServerMethodDefinition.create(fake, call);

        return definition;
    }
}
