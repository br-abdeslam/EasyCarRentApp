package be.condorcet.easycarrent.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Year;

/**
 * Enforces {@link ManufacturingYear}: an optional year that, when present, must
 * be between {@code min} and the current year inclusive.
 */
public class ManufacturingYearValidator implements ConstraintValidator<ManufacturingYear, Integer> {

    private int min;

    @Override
    public void initialize(ManufacturingYear constraint) {
        this.min = constraint.min();
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int currentYear = Year.now().getValue();
        return value >= min && value <= currentYear;
    }
}
