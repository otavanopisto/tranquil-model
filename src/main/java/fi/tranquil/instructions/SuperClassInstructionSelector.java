package fi.tranquil.instructions;

import fi.tranquil.TranquilizingContext;

public class SuperClassInstructionSelector implements InstructionSelector {

  public SuperClassInstructionSelector(Class<?> entityClass) {
    this.entityClass = entityClass;
  }
  
  @Override
  public boolean match(TranquilizingContext context) {
    if ((entityClass == null)||(context.getEntityClass() == null)) {
      return false;
    }
    
    return this.entityClass.isAssignableFrom(context.getEntityClass());
  }
  
  private Class<?> entityClass;
}
