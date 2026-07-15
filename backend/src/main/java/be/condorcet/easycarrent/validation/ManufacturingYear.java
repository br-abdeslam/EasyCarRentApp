package be.condorcet.easycarrent.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a manufacturing year is within a reasonable range and not in
 * the future.
 *
 * <p>The upper bound is resolved at validation time from the system clock, so it
 * stays correct as calendar years change instead of hardcoding a year into a
 * static annotation. A {@code null} value is considered valid; combine with
 * {@code @NotNull} when the year is mandatory.
 */
@Documented
@Constraint(validatedBy = ManufacturingYearValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
        ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ManufacturingYear {

    String message() default "manufacturingYear must be a valid year that is not in the future";

    int min() default 1900;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
