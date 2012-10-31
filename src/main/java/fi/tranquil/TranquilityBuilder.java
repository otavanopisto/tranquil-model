package fi.tranquil;

import fi.tranquil.instructions.PropertySkipInstruction;
import fi.tranquil.instructions.PropertyTypeInstruction;
import fi.tranquil.instructions.impl.PropertySkipInstructionImpl;
import fi.tranquil.instructions.impl.PropertyTypeInstructionImpl;
import fi.tranquil.processing.PropertyAccessor;
import fi.tranquil.processing.TranquilityEntityFactory;

public class TranquilityBuilder {
  
  public TranquilityBuilder(PropertyAccessor propertyAccessor, TranquilityEntityFactory tranquilityEntityFactory) {
    this.propertyAccessor = propertyAccessor;
    this.tranquilityEntityFactory = tranquilityEntityFactory;
  }

  public Tranquility createTranquility() {
    return new TranquilityImpl(propertyAccessor, tranquilityEntityFactory);
  }
  
  public PropertyTypeInstruction createPropetyTypeInstruction(TranquilModelType type) {
    return new PropertyTypeInstructionImpl(type);
  }

  public PropertySkipInstruction createPropetySkipInstruction() {
    return new PropertySkipInstructionImpl();
  }
  
  private TranquilityEntityFactory tranquilityEntityFactory;
  private PropertyAccessor propertyAccessor;
}