package fi.tranquil.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewMethod;
import javassist.NotFoundException;

import javax.persistence.Entity;

import fi.tranquil.TranquilEntity;
import fi.tranquil.TranquilModel;
import fi.tranquil.TranquilModelEntity;
import fi.tranquil.TranquilModelType;
import fi.tranquil.Tranquility;
import fi.tranquil.TranquilizingContext;
import fi.tranquil.instructions.ClassInstructionSelector;
import fi.tranquil.instructions.Instruction;
import fi.tranquil.instructions.InstructionSelector;
import fi.tranquil.instructions.PathInstructionSelector;
import fi.tranquil.instructions.PropertyInjectInstruction;
import fi.tranquil.instructions.PropertyInjectInstruction.ValueGetter;
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
    Class<?> entityClass = entity.getClass();
    return tranquilizeEntity(new TranquilizingContext(entityClass, entity, ""), entity);
  }
   
  @Override
  public TranquilModelEntity[] entities(Object[] entities) {
    if (entities == null)
      return null;
    
    if (entities.length == 0)
      return new TranquilModelEntity[0];
    
    Class<?> entityClass = entities[0].getClass();
    
    return tranquilizeEntities(new TranquilizingContext(entityClass, entities, ""), entities);
  }
  
  @Override
  public Collection<TranquilModelEntity> entities(Collection<?> entities) {
    if (entities == null)
      return null;
    
    if (entities.isEmpty()) {
      return new ArrayList<>();
    }
    
    Class<?> entityClass = entities.iterator().next().getClass();
    
    return tranquilizeEntities(new TranquilizingContext(entityClass, entities, ""), entities);
  }
  
  @Override
  public Tranquility addInstruction(Instruction instruction) {
    return addInstruction("", instruction);
  }

  @Override
  public Tranquility addInstruction(String path, Instruction instruction) {
    return addInstruction(new PathInstructionSelector(path), instruction);
  }

  @Override
  public Tranquility addInstruction(Class<?> entityClass, Instruction instruction) {
    return addInstruction(new ClassInstructionSelector(entityClass), instruction);
  }

  public Tranquility addInstruction(InstructionSelector selector, Instruction instruction) {
    instructions.add(new InstructionItem(selector, instruction));
    return this;
  }
  
  private TranquilModelEntity tranquilizeEntity(TranquilizingContext context, Object entity) {
    List<Instruction> pathInstructions = resolveInstructions(context);
    TranquilModelType type = resolveEntityType(pathInstructions, TranquilModelType.COMPACT);
    
    Class<?> tranquilModelClass = tranquilityEntityFactory.findTranquileModel(entity.getClass(), type);
    if (tranquilModelClass != null) {
      Map<String, PropertyInjectInstruction.ValueGetter<?>> injectedProperties = resolveInjectedProperties(pathInstructions);
      TranquilModelEntity tranquilModel = getTranquilModelEntity(tranquilModelClass, injectedProperties);
      
      do {
        TranquilModel modelAnnotation = tranquilModelClass.getAnnotation(TranquilModel.class);

        String[] modelProperties = getModelProperties(tranquilModelClass);
        for (String modelProperty : modelProperties) {
          Class<?> propertyClass = propertyAccessor.getFieldType(entity.getClass(), modelProperty);
          String propertyPath = ("".equals(context.getPath()) ? "" : context.getPath() + ".") + modelProperty;
          TranquilizingContext propertyContext = new TranquilizingContext(context, propertyClass, null, propertyPath);
          List<Instruction> propertyInstructions = resolveInstructions(propertyContext);
          
          boolean skip = resolveSkip(propertyInstructions);
          if (!skip) {
            Object entityValue = propertyAccessor.extractProperty(entity, modelProperty);
            if (entityValue != null) {
              propertyContext.setEntityValue(entityValue);
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
                    TranquilModelEntity tranquilizedEntity = tranquilizeEntity(propertyContext, entityValue);
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
                    Collection<TranquilModelEntity> tranquilizedEntities = tranquilizeEntities(propertyContext, (Collection<?>) entityValue);
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
                    Collection<TranquilModelEntity> tranquilizedEntities = tranquilizeEntities(propertyContext, (Collection<?>) entityValue);
                    propertyAccessor.storeProperty(tranquilModel, modelProperty, tranquilizedEntities);
                  break;
                }
              	
              	TranquilModelEntity[] tranquilizedEntities = tranquilizeEntities(propertyContext, (Object[]) entityValue);
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

      // Inject injected properties into model class
      Set<String> injectedPropertyKeys = injectedProperties.keySet();
      for (String injectedPropertyKey : injectedPropertyKeys) {
        ValueGetter<?> valueGetter = injectedProperties.get(injectedPropertyKey);
        propertyAccessor.storeProperty(tranquilModel, injectedPropertyKey, valueGetter.getValue(context)); 
      }

      return tranquilModel;
    } else {
      // TranquilModelEntity not found
      return null;
    }
  }

  private TranquilModelEntity[] tranquilizeEntities(TranquilizingContext context, Object[] entities) {
    TranquilModelEntity[] result = new TranquilModelEntity[entities.length];
    for (int i = 0, l = entities.length; i < l; i++) {
      result[i] = tranquilizeEntity(context, entities[i]);
    }

    return result;
  }

  private Collection<TranquilModelEntity> tranquilizeEntities(TranquilizingContext context, Collection<?> entities) {
    List<TranquilModelEntity> result = new ArrayList<TranquilModelEntity>();
    for (Object entity : entities) {
      result.add(tranquilizeEntity(context, entity));
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
  
  private Map<String, PropertyInjectInstruction.ValueGetter<?>> resolveInjectedProperties(List<Instruction> instructions) {
    Map<String, PropertyInjectInstruction.ValueGetter<?>> injectedProperties = new HashMap<>();
  
    // Iterate over instructions to find injected properties
    
    for (Instruction instruction : instructions) {
      if (instruction instanceof PropertyInjectInstruction) {
        PropertyInjectInstruction<?> injectInstruction = (PropertyInjectInstruction<?>) instruction;
        PropertyInjectInstruction.ValueGetter<?> valueGetter = injectInstruction.getValueGetter();
        injectedProperties.put(injectInstruction.getName(), valueGetter);
      }
    }
    
    return injectedProperties;
  }

  private TranquilModelEntity getTranquilModelEntity(Class<?> tranquilModelClass, Map<String, PropertyInjectInstruction.ValueGetter<?>> injectedProperties) {
    
    try {
      // If no injected properties have been found, we can use original class
      Set<String> injectedPropertyKeys = injectedProperties.keySet();
      if (injectedPropertyKeys.isEmpty()) {
        return (TranquilModelEntity) tranquilModelClass.newInstance();
      } else {
        // Otherwise we need to extend class with given properties...
        ClassPool classPool = getClassPool(tranquilModelClass);
        
        String originalClassName = tranquilModelClass.getName(); 

        CtClass originalCtClass = classPool.get(originalClassName);
        CtClass extendedCtClass = classPool.makeClass(tranquilModelClass.getName() + "$tranquil-" + UUID.randomUUID().toString(), originalCtClass);
        
        for (String injectedPropertyKey : injectedPropertyKeys) {
          ValueGetter<?> injectedValueGetter = injectedProperties.get(injectedPropertyKey);
          Method getValueMethod = injectedValueGetter.getClass().getMethod("getValue", TranquilizingContext.class);
          Class<?> injectedPropertyType = getValueMethod.getReturnType();
          String capitalizedPropertyKey = capitalize(injectedPropertyKey);
          
          // Field
          CtField field = CtField.make("private " + injectedPropertyType.getName() + " " + injectedPropertyKey + ";", extendedCtClass);
          extendedCtClass.addField(field); 
          // Getter
          extendedCtClass.addMethod(CtNewMethod.getter("get" + capitalizedPropertyKey, field));
          // Setter
          extendedCtClass.addMethod(CtNewMethod.setter("set" + capitalizedPropertyKey, field));
        }

        Class<?> extendedClass = extendedCtClass.toClass(getClass().getClassLoader(), getClass().getProtectionDomain());
      
        return (TranquilModelEntity) extendedClass.newInstance();
      }
    } catch (InstantiationException e) {
      // Failed to initialize TranquilModelEntity.
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      // Failed to inject TranquilModelEntity property
      throw new RuntimeException(e);
    } catch (NotFoundException e) {
      // Javassist could not find original class
      throw new RuntimeException(e);
    } catch (CannotCompileException e) {
      // Javassist could not compile extended class
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
     // Could not find value getter getMethod
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      // Could not find value getter getMethod
      throw new RuntimeException(e);
    }
  }

  private synchronized ClassPool getClassPool(Class<?> tranquilModelClass) {
    if (classPool == null) {
      classPool = ClassPool.getDefault();
      classPool.insertClassPath(new ClassClassPath(tranquilModelClass));
    }
    
    return classPool;
  }
  
  private String capitalize(String string) {
    return Character.toUpperCase(string.charAt(0)) + string.substring(1);
  }
  
  private List<Instruction> resolveInstructions(TranquilizingContext context) {
    List<Instruction> result = new ArrayList<Instruction>();
    for (InstructionItem item : instructions) {
      if (item.getSelector().match(context))
        result.add(item.getInstruction());
    }
    
    return result;
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

  private List<InstructionItem> instructions = new ArrayList<>();
  private PropertyAccessor propertyAccessor;
  private TranquilityEntityFactory tranquilityEntityFactory;
  private static ClassPool classPool = null;
  
  private class InstructionItem {
    
    public InstructionItem(InstructionSelector selector, Instruction instruction) {
      this.selector = selector;
      this.instruction = instruction;
    }
    
    public Instruction getInstruction() {
      return instruction;
    }
    
    public InstructionSelector getSelector() {
      return selector;
    }
    
    private Instruction instruction;
    private InstructionSelector selector;
  }
}