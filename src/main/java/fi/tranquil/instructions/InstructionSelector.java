package fi.tranquil.instructions;

import fi.tranquil.impl.TranquilizingContext;

public interface InstructionSelector {

  public boolean match(TranquilizingContext context);
  
}
