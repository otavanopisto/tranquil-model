package fi.tranquil.instructions.impl;

import fi.tranquil.instructions.PropertyInjectInstruction;

public class PropertyInjectInstructionImpl<T> implements PropertyInjectInstruction<T> {
  
  public PropertyInjectInstructionImpl(String name, fi.tranquil.instructions.PropertyInjectInstruction.ValueGetter<T> valueGetter) {
    this.name = name;
    this.valueGetter = valueGetter;
  }
  
  @Override
  public String getName() {
    return name;
  }
  
  @Override
  public fi.tranquil.instructions.PropertyInjectInstruction.ValueGetter<T> getValueGetter() {
    return valueGetter;
  }
  
  private String name;
  private fi.tranquil.instructions.PropertyInjectInstruction.ValueGetter<T> valueGetter;
}
