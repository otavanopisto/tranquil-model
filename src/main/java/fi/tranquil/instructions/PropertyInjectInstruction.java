package fi.tranquil.instructions;

import fi.tranquil.TranquilizingContext;

public interface PropertyInjectInstruction <T> extends Instruction {

  public String getName();
  public ValueGetter<T> getValueGetter();
  
  public interface ValueGetter<T> {
    
    public T getValue(TranquilizingContext context);
  
  }
  
}
