package fi.tranquil.instructions;

import fi.tranquil.TranquilizingContext;

public class ClassInstructionSelector implements InstructionSelector {

  public ClassInstructionSelector(Class<?> entityClass) {
    this.entityClass = entityClass;
  }
  
  @Override
  public boolean match(TranquilizingContext context) {
    if (entityClass == null) {
      return context.getEntityClass() == null;
    }
    
    if (context.getEntityClass() == null) {
      return false;
    }
    
    return context.getEntityClass().equals(entityClass);
  }
  
  private Class<?> entityClass;
}
