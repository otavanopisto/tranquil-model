package fi.tranquil.processing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import fi.tranquil.TranquilEntityResolver;

/**
 * Annotation for expanded tranquil entity field. Field with this annotation in
 * complete model will be evaluated with resolver during runtime. This will be
 * used only for processing. For defining, use TranquilEntityField.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface TranquilityExpandedField {

  Class<? extends TranquilEntityResolver> entityResolverClass();

  String idProperty();
}
