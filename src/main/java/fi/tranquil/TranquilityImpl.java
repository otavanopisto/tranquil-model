package fi.tranquil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;

import fi.tranquil.instructions.Instruction;
import fi.tranquil.instructions.PropertySkipInstruction;
import fi.tranquil.instructions.PropertyTypeInstruction;
import fi.tranquil.processing.PropertyAccessor;
import fi.tranquil.processing.TranquilityEntityFactory;

public class TranquilityImpl implements Tranquility {

  public TranquilityImpl(PropertyAccessor propertyAccessor, TranquilityEntityFactory tranquilityEntityFactory) {
    this.propertyAccessor = propertyAccessor;
    this.tranquilityEntityFactory = tranquilityEntityFactory;
  }
  
  @Override
  public TranquilModelEntity entity(Object entity) {
    return tranquilizeEntity("", entity);
  }
   
  @Override
  public TranquilModelEntity[] entities(Object[] entities) {
    return tranquilizeEntities("", entities);
  }
  
  @Override
  public Collection<TranquilModelEntity> entities(Collection<?> entities) {
    return tranquilizeEntities("", entities);
  }
  
  @Override
  public Tranquility addInstruction(Instruction instruction) {
    return addInstruction("", instruction);
  }

  @Override
  public Tranquility addInstruction(String path, Instruction instruction) {
    getInstructionsList(path).add(instruction);
    return this;
  }

  private TranquilModelEntity tranquilizeEntity(String path, Object entity) {
    List<Instruction> pathInstructions = getInstructionsList(path);
    TranquilModelType type = resolveEntityType(pathInstructions, TranquilModelType.COMPACT);
    
    Class<?> tranquilModelClass = tranquilityEntityFactory.findTranquileModel(entity.getClass(), type);
    if (tranquilModelClass != null) {
      try {
        TranquilModelEntity tranquilModel = (TranquilModelEntity) tranquilModelClass.newInstance();

        do {
          TranquilModel modelAnnotation = tranquilModelClass.getAnnotation(TranquilModel.class);

          String[] modelProperties = getModelProperties(tranquilModelClass);
          for (String modelProperty : modelProperties) {
            
            String propertyPath = ("".equals(path) ? "" : path + ".") + modelProperty;
            List<Instruction> propertyInstructions = getInstructionsList(propertyPath);
            
            boolean skip = resolveSkip(propertyInstructions);
            if (!skip) {
              Object entityValue = propertyAccessor.extractProperty(entity, modelProperty);
              if (entityValue != null) {
                Class<?> entityClass = entityValue.getClass();
                
                if (isEntity(entityClass)) {
                  // Entity properties need to be converted into their tranquilized counterparts or id properties
  
                  switch (modelAnnotation.entityType()) {
                    case BASE:
                    // There should not be entities when processing base...
                    break;
                    case COMPACT:
                      // Compact versions contain only ids of entities. So we move only that.
                      Object id = propertyAccessor.extractProperty(entityValue, "id");
                      if (id != null) {
                        propertyAccessor.storeProperty(tranquilModel, modelProperty + "Id", id);
                      }
                    break;
                    case COMPLETE:
                      TranquilModelEntity tranquilizedEntity = tranquilizeEntity(propertyPath, entityValue);
                      if (tranquilizedEntity != null) {
                        propertyAccessor.storeProperty(tranquilModel, modelProperty, tranquilizedEntity);
                      }
                    break;
                  }
                } else if (isCollection(entityClass)) {
                  switch (modelAnnotation.entityType()) {
                    case BASE:
                      // Simple collections are copied "as-is"
                      propertyAccessor.storeProperty(tranquilModel, modelProperty, entityValue);
                    break;
                    case COMPACT:
                      // Compact  collections are stored as lists of ids

                      List<Object> ids = new ArrayList<>();
                    	
                      for (Object collectionItem : (Collection<?>) entityValue) {
                    	Object id = propertyAccessor.extractProperty(collectionItem, "id");
                    	if (id != null)
                    	  ids.add(id);
                      }
                      
                      propertyAccessor.storeProperty(tranquilModel, modelProperty + "Ids", ids);
                    break;
                    case COMPLETE:
                      // Complete collections are stored as tranqulized entities
                      Collection<TranquilModelEntity> tranquilizedEntities = tranquilizeEntities(propertyPath, (Collection<?>) entityValue);
                      propertyAccessor.storeProperty(tranquilModel, modelProperty, tranquilizedEntities);
                    break;
                  }
                  
                } else if (isArray(entityClass)) {
                  switch (modelAnnotation.entityType()) {
                    case BASE:
                      // Simple arrays are copied "as-is"
                      propertyAccessor.storeProperty(tranquilModel, modelProperty, entityValue);
                    break;
                    case COMPACT:
                      // Compact arrays are stored as arrays of ids
                      Object[] listItems = (Object[]) entityValue;
                      Object[] ids = new Object[listItems.length];
                    	for (int i = 0, l = listItems.length; i < l; i++) {
                        Object id = propertyAccessor.extractProperty(listItems[i], "id");
                    	  ids[i] = id;
                    	}
                      
                      propertyAccessor.storeProperty(tranquilModel, modelProperty + "Ids", ids);
                    break;
                    case COMPLETE:
                      // Complete collections are stored as tranqualized entities
                      Collection<TranquilModelEntity> tranquilizedEntities = tranquilizeEntities(propertyPath, (Collection<?>) entityValue);
                      propertyAccessor.storeProperty(tranquilModel, modelProperty, tranquilizedEntities);
                    break;
                  }
                	
                	TranquilModelEntity[] tranquilizedEntities = tranquilizeEntities(propertyPath, (Object[]) entityValue);
                  propertyAccessor.storeProperty(tranquilModel, modelProperty, tranquilizedEntities);

                } else {
                  // Simple properties are moved as is.
                  propertyAccessor.storeProperty(tranquilModel, modelProperty, entityValue);
                }
              }
            }
          }

          tranquilModelClass = tranquilModelClass.getSuperclass();
        } while (tranquilModelClass != null && !Object.class.equals(tranquilModelClass));

        return tranquilModel;
      } catch (InstantiationException e) {
        // Failed to initialize TranquilModelEntity.
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        // Failed to inject TranquilModelEntity property
        throw new RuntimeException(e);
      }
    } else {
      // TranquilModelEntity not found
      return null;
    }
  }

  private TranquilModelEntity[] tranquilizeEntities(String path, Object[] entities) {
    TranquilModelEntity[] result = new TranquilModelEntity[entities.length];
    for (int i = 0, l = entities.length; i < l; i++) {
      result[i] = tranquilizeEntity(path, entities[i]);
    }

    return result;
  }

  private Collection<TranquilModelEntity> tranquilizeEntities(String path, Collection<?> entities) {
    List<TranquilModelEntity> result = new ArrayList<TranquilModelEntity>();
    for (Object entity : entities) {
      result.add(tranquilizeEntity(path, entity));
    }
    return result;
  }
  
  private boolean resolveSkip(List<Instruction> instructions) {
    for (Instruction instruction : instructions) {
      if (instruction instanceof PropertySkipInstruction) {
        return true;
      }
    }

    return false;
  }

  private TranquilModelType resolveEntityType(List<Instruction> instructions, TranquilModelType defaultType) {
    for (Instruction instruction : instructions) {
      if (instruction instanceof PropertyTypeInstruction) {
        return ((PropertyTypeInstruction) instruction).getType();
      }
    }

    return defaultType;
  }
  
  private List<Instruction> getInstructionsList(String path) {
    List<Instruction> instructions = this.instructions.get(path);
    if (instructions == null) {
      instructions = new ArrayList<Instruction>();
      this.instructions.put(path, instructions);
    }
    
    return instructions;
  }

  private String[] getModelProperties(Class<?> modelClass) {
    try {
      return (String[]) modelClass.getField("properties").get(null);
    } catch (Exception e) {
      return null;
    }
  }

  private boolean isArray(Class<?> entityClass) {
    return entityClass.isArray();
  }

  private boolean isCollection(Class<?> entityClass) {
    return java.util.Collection.class.isAssignableFrom(entityClass);
  }

  private boolean isEntity(Class<?> entityClass) {
    return entityClass.isAnnotationPresent(TranquilEntity.class) || entityClass.isAnnotationPresent(Entity.class);
  }

  private Map<String, List<Instruction>> instructions = new HashMap<String, List<Instruction>>();
  private PropertyAccessor propertyAccessor;
  private TranquilityEntityFactory tranquilityEntityFactory;
}
