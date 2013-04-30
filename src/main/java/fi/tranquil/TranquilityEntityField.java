package fi.tranquil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;

/**
 * Marks a field of tranquility entity to be resolved into another tranquility
 * entity. Tranquility generator will reserve a field for resolved entity in
 * complete element which will be resolved using resolver during runtime.
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface TranquilityEntityField {

  @Nonbinding
  Class<? extends TranquilEntityResolver> value();

  String fieldName() default "";
}
