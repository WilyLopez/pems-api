package com.playzone.pems.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ContactoAdicionalValidator.ContactoAdicionalConstraintValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContactoAdicionalValidator {

    String message() default "El contacto adicional debe ser un celular (9XXXXXXXX) o un correo válido.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class ContactoAdicionalConstraintValidator implements ConstraintValidator<ContactoAdicionalValidator, String> {

        private static final String PATRON = "^(9\\d{8}|[^\\s@]+@[^\\s@]+\\.[^\\s@]+)$";

        @Override
        public boolean isValid(String contacto, ConstraintValidatorContext context) {
            if (contacto == null || contacto.isBlank()) {
                return true;
            }
            return contacto.matches(PATRON);
        }
    }
}
