package fi.tranquil.instructions;

import fi.tranquil.TranquilizingContext;

public interface InstructionSelector {

  public boolean match(TranquilizingContext context);
  
}
