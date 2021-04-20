import com.google.protobuf.Message.*;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.Marshaller;
import io.grpc.ServerCallHandler;
import static io.grpc.stub.ServerCalls.*;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;

@Slf4j
public class gRPCUtils {

    public static MethodDescriptor<?, ?> findMethodDescriptor(String serviceName, String methodName) {

        // aqui sera definido como ira encontrar o sevico/metodo grpc
        // neste caso como a definicao ja esta no classloader, nao será necessario implementar
        // uma regra de procura complexa

        // toda classe gerada pelo grpc existe um padrao de nomeclatura
        // servico sempre eh 'nome do servico' + 'Grpc' - GreeterServiceGrpc
        // o objetivo eh obter a definicao real do metodo no grpc e existe um metodo na classe para isto
        // metodo sempre eh 'get' + 'nome do metodo' + 'Method' - getSayHelloUnaryMethod

        ClassLoader loader = null;
        Class<?> clazz = null;
        Method method = null;
        MethodDescriptor<?, ?> descriptor = null;

        try {

            loader = gRPCUtils.class.getClassLoader();
            clazz = loader.loadClass(serviceName + "Grpc");
            method = clazz.getDeclaredMethod("get" + methodName + "Method");
            descriptor = (MethodDescriptor<?, ?>) method.invoke(null);

            if (descriptor == null)
                log.error(MessageFormat.format("Método ''{1}'' não encontrado no serviço ''{0}''!", serviceName, methodName));

            return descriptor;

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException ex) {
            log.error(MessageFormat.format("Erro ao procurar o método gRPC {0} do serviço {1}", methodName, serviceName), ex);
            return null;
        }
    }

    public static MethodDescriptor<Object, Object> makeFakeMethod(MethodDescriptor<?, ?> source) {

        String errorMessage = "Erro ao executar o método {0} na classe {0}";
        String className = Marshaller.class.getCanonicalName();
        Class<?> clazz = source.getClass();

        Marshaller<Object> requestMarshaller = new Marshaller<>() {
            @Override
            public InputStream stream(Object o) {
                try {
                    Method streamRequest = clazz.getDeclaredMethod("streamRequest", Object.class);
                    return (InputStream) streamRequest.invoke(source, o);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                    log.error(MessageFormat.format(errorMessage, "streamRequest", className), ex);
                    return null;
                }
            }

            @Override
            public Object parse(InputStream inputStream) {
                return source.parseRequest(inputStream);
            }
        };

        Marshaller<Object> responseMarshaller = new Marshaller<>() {
            @Override
            public InputStream stream(Object o) {
                try {
                    Method streamResponse = clazz.getDeclaredMethod("streamResponse", Object.class);
                    return (InputStream) streamResponse.invoke(source, o);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                    log.error(MessageFormat.format(errorMessage, "streamResponse", className), ex);
                    return null;
                }
            }

            @Override
            public Object parse(InputStream inputStream) {
                return source.parseResponse(inputStream);
            }
        };

        return MethodDescriptor.newBuilder(requestMarshaller, responseMarshaller)
                .setFullMethodName(source.getFullMethodName())
                .setType(source.getType())
                .setSchemaDescriptor(source.getSchemaDescriptor())
                .setIdempotent(source.isIdempotent())
                .setSampledToLocalTracing(source.isSampledToLocalTracing())
                .setSafe(source.isSafe())
                .build();
    }

    public static ServerCallHandler<Object, Object> getServerCallHandlerByType(MethodDescriptor<?, ?> descriptor) {

        ServerCallHandler<Object, Object> call;
        ProxyMethodHandler<Object, Object> handler = new ProxyMethodHandler<>(descriptor);

        switch (descriptor.getType()) {
            case UNARY:
                call = asyncUnaryCall(handler); break;
            case CLIENT_STREAMING:
                call = asyncClientStreamingCall(handler); break;
            case SERVER_STREAMING:
                call = asyncServerStreamingCall(handler); break;
            case BIDI_STREAMING:
                call = asyncBidiStreamingCall(handler); break;
            default:
                call = null;
        }

        return call;
    }

    public static Class<?> getClassFromMarshaller(Marshaller<?> marshaller) {

        try {
            Method getMessageClass = marshaller.getClass().getMethod("getMessageClass");
            getMessageClass.setAccessible(true);
            return (Class<?>) getMessageClass.invoke(marshaller);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            return  null;
        }

    }

    public static Builder getBuilderFromMarshaller(Marshaller<?> marshaller) {

        // utilizado para construir response no tipo nativo do protobuf

        try {
            Class<?> clazz = getClassFromMarshaller(marshaller);
            assert clazz != null;
            Method method = clazz.getMethod("newBuilder");
            return (Builder) method.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            return null;
        }
    }
}
