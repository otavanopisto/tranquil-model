package fi.tranquil.instructions.impl;

import fi.tranquil.TranquilModelType;
import fi.tranquil.instructions.PropertyTypeInstruction;

public class PropertyTypeInstructionImpl implements PropertyTypeInstruction {

  public PropertyTypeInstructionImpl(TranquilModelType type) {
    this.type = type;
  }
  
  public TranquilModelType getType() {
    return this.type;
  };
  
  private TranquilModelType type;
}
