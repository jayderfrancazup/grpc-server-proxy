import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataUtils {

    public static JsonNode lookupResponse(JsonNode json) {

        log.info("Processando a resposta da requição do serviço");

        String firstName = json.get("firstName").asText().toLowerCase();
        String lastName = json.get("lastName").asText().toLowerCase();

        String message = "Não reconheco seu nome, então sem saudações para você!";


        if (firstName.equals("jayder") && lastName.equals("frança"))
            message = "Olá Jayder Evaristo França!";
        else if (firstName.equals("túlio") && lastName.equals("teixeira"))
            message = "Olá TULIO!";

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        result.put("message", message);
        return result;
    }
}
