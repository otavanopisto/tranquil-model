package fi.tranquil;

public @interface TranquilEntityPropertyDefaults {
  
  boolean skip() default false;
  
  TranquilModelType type() default TranquilModelType.COMPACT;
  
}
