package fi.tranquil.instructions.impl;

import fi.tranquil.instructions.PropertyInjectInstruction;

public class PropertyInjectInstructionImpl implements PropertyInjectInstruction {
  
  public PropertyInjectInstructionImpl(String name, Object value) {
    this.name = name;
    this.value = value;
  }
  
  @Override
  public String getName() {
    return name;
  }
  
  @Override
  public Object getValue() {
    return value;
  }
  
  private String name;
  private Object value;

}
