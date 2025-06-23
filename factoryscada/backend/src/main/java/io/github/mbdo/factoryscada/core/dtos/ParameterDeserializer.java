package io.github.mbdo.factoryscada.core.dtos;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mbdo.factoryscada.core.Passable;
import io.github.mbdo.factoryscada.core.enums.PassableType;
import io.github.mbdo.factoryscada.core.passable.BoxNumber;
import io.github.mbdo.factoryscada.core.passable.Color;
import io.github.mbdo.factoryscada.core.passable.Direction;
import io.github.mbdo.factoryscada.core.passable.NumberNatural;
import io.github.mbdo.factoryscada.core.passable.PositionParameterThreeD;
import io.github.mbdo.factoryscada.core.passable.AxisPrioritized;
import io.github.mbdo.factoryscada.core.passable.MPSOutput;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Set;

@Slf4j
public class ParameterDeserializer extends JsonDeserializer<Parameter> {
    @Override
    public Parameter deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode node = parser.getCodec().readTree(parser);

        PassableType passableType = mapper.treeToValue(node.get("passableType"), PassableType.class);
        Passable passable = switch (passableType) {
            case COLOR -> mapper.treeToValue(node.get("passable"), Color.class);
            case DIRECTION -> mapper.treeToValue(node.get("passable"), Direction.class);
            case BOXNUMBER -> mapper.treeToValue(node.get("passable"), BoxNumber.class);
            case POSITIONPARAMETERTHREED -> mapper.treeToValue(node.get("passable"), PositionParameterThreeD.class);
            case NUMBERNATURAL -> mapper.treeToValue(node.get("passable"), NumberNatural.class);
            case AXISPRIORITIZED -> mapper.treeToValue(node.get("passable"), AxisPrioritized.class);
            case MPSOUTPUT -> mapper.treeToValue(node.get("passable"), MPSOutput.class);
            default -> {
                log.error("Unknown passable type: {}", passableType);
                throw new IllegalArgumentException("Unknown passable type: " + passableType);
            }
        };
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        Set<ConstraintViolation<Passable>> violations = validator.validate(passable);
        for (ConstraintViolation<Passable> violation : violations) {
            log.error("invalid Passable " + node.get("passable") +" " +violation.getMessage());
            throw new IllegalArgumentException("invalid passable: " + node.get("passable"));
        }
        
        return new Parameter(passableType, passable);
    }
}
