package pl.wolski.bank.annotations;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniquePersonalIdentificationNumberValidator.class)
public @interface UniquePersonalIdentificationNumber {
    String message() default "Personal identifiation number not unique";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
