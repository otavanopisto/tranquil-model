package fi.tranquil;

import java.util.Collection;

import fi.tranquil.instructions.Instruction;

public interface Tranquility {

  public TranquilModelEntity entity(Object entity);
  public TranquilModelEntity[] entities(Object[] entities);
  public Collection<TranquilModelEntity> entities(Collection<?> entities);
  
  public Tranquility addInstruction(Instruction instruction);
  public Tranquility addInstruction(String path, Instruction instruction);
  
}
