package fi.tranquil;

import java.util.Collection;

import fi.tranquil.instructions.Instruction;
import fi.tranquil.instructions.InstructionSelector;

public interface Tranquility {

  public TranquilModelEntity entity(Object entity);
  public TranquilModelEntity[] entities(Object[] entities);
  public Collection<TranquilModelEntity> entities(Collection<?> entities);
  
  /**
   * Adds root entity instruction. Shortcut for addInstruction("", instruction);
   * 
   * @param instruction instruction
   * @return self
   */
  public Tranquility addInstruction(Instruction instruction);
  
  /**
   * Adds new path instruction. Shortcut for addInstruction(new PathInstructionSelector(path), instruction);
   * 
   * @param path path where instruction should be applied 
   * @param instruction
   * @return self
   */
  public Tranquility addInstruction(String path, Instruction instruction);

  /**
   * Adds new entity class instruction. Shortcut for addInstruction(new ClassInstructionSelector(entityClass), instruction);
   * 
   * @param entityClass entity class in what instruction should be applied.
   * @param instruction instruction
   * @return self
   */
  public Tranquility addInstruction(Class<?> entityClass, Instruction instruction);
  
  /**
   * Adds new instruction. 
   * 
   * @param selector selector to be used for determining where instruction should be applied 
   * @param instruction instruction
   * @return self
   */
  public Tranquility addInstruction(InstructionSelector selector, Instruction instruction);
}
