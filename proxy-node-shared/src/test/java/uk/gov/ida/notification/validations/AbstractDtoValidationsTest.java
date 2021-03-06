package uk.gov.ida.notification.validations;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractDtoValidationsTest<DTO> {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    protected Map<String, List<ConstraintViolation<DTO>>> validateAndMap(DTO dto) {
        return mapViolations(VALIDATOR.validate(dto));
    }

    private Map<String, List<ConstraintViolation<DTO>>> mapViolations(Set<ConstraintViolation<DTO>> violations) {
        Map<String, List<ConstraintViolation<DTO>>> violationMap = new HashMap<>();
        for (ConstraintViolation<DTO> violation : violations) {
            String path = violation.getPropertyPath().toString();
            violationMap.computeIfAbsent(path, k -> new LinkedList<>());
            violationMap.get(path).add(violation);
        }

        return violationMap;
    }
}
