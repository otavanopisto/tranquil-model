package fi.tranquil.instructions;

public interface PropertyInjectInstruction extends Instruction {

  public String getName();
  public Object getValue();
  
}
