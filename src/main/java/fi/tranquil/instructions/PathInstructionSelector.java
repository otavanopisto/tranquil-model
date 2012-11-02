package fi.tranquil.instructions;

import fi.tranquil.TranquilizingContext;

public class PathInstructionSelector implements InstructionSelector {

  public PathInstructionSelector(String path) {
    this.path = path;
  }
  
  @Override
  public boolean match(TranquilizingContext context) {
    if (path == null) {
      return context.getPath() == null;
    }
    
    if (context.getPath() == null) {
      return false;
    }
    
    return context.getPath().equals(path);
  }
  
  private String path;
}
