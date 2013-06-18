package fi.tranquil.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
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
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import javax.persistence.Entity;

import fi.tranquil.TranquilEntity;
import fi.tranquil.TranquilEntityResolver;
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
import fi.tranquil.processing.TranquilityExpandedField;

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
    
    Class<?> entityClass = entities.getClass();
    
    return tranquilizeEntities(new TranquilizingContext(entityClass, entities, ""), entities);
  }
  
  @Override
  public Collection<TranquilModelEntity> entities(Collection<?> entities) {
    if (entities == null)
      return null;
    
    if (entities.isEmpty()) {
      return new ArrayList<>();
    }
    
    Class<?> entityClass = entities.getClass();
    
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
    List<Instruction> instructions = resolveInstructions(context);
    TranquilModelType type = resolveEntityType(instructions, TranquilModelType.COMPACT);
    
    Class<?> tranquilModelClass = tranquilityEntityFactory.findTranquilModel(entity.getClass(), type);
    if (tranquilModelClass != null) {
      Map<String, PropertyInjectInstruction.ValueGetter<?>> injectedProperties = resolveInjectedProperties(instructions);
      TranquilModelEntity tranquilModel = getTranquilModelEntity(tranquilModelClass, injectedProperties);
      
      do {
        TranquilModel modelAnnotation = tranquilModelClass.getAnnotation(TranquilModel.class);
        
        String[] modelProperties = getModelProperties(tranquilModelClass);
        for (String modelProperty : modelProperties) {
          Object entityPropertyValue = null;
          boolean expandedProperty = (modelAnnotation.entityType() == TranquilModelType.COMPLETE) && isExpandedProperty(tranquilModelClass, modelProperty);

          Class<?> propertyClass;
          if (!expandedProperty)
            propertyClass = propertyAccessor.getFieldType(entity.getClass(), modelProperty);
          else {
            entityPropertyValue = loadEntity(entity, tranquilModelClass, modelProperty);
            propertyClass = entityPropertyValue.getClass();
          }
          
          String propertyPath = ("".equals(context.getPath()) ? "" : context.getPath() + ".") + modelProperty;
          TranquilizingContext propertyContext = new TranquilizingContext(context, propertyClass, null, propertyPath);
          List<Instruction> propertyInstructions = resolveInstructions(propertyContext);
          
          boolean skip = resolveSkip(propertyInstructions);
          if (!skip) {

            if (!expandedProperty)
              entityPropertyValue = propertyAccessor.extractProperty(entity, modelProperty);
            
            if (entityPropertyValue != null) {
              propertyContext.setEntityValue(entityPropertyValue);
              Class<?> entityClass = entityPropertyValue.getClass();
              
              if (isEntity(entityClass)) {
                // Entity properties need to be converted into their tranquilized counterparts or id properties

                switch (modelAnnotation.entityType()) {
                  case BASE:
                  // There should not be entities when processing base...
                  break;
                  case COMPACT:
                    // Compact versions contain only ids of entities. So we move only that.
                    Object id = propertyAccessor.extractProperty(entityPropertyValue, "id");
                    if (id != null) {
                      propertyAccessor.storeProperty(tranquilModel, modelProperty + "_id", id);
                    }
                  break;
                  case COMPLETE:
                    TranquilModelEntity tranquilizedEntity = tranquilizeEntity(propertyContext, entityPropertyValue);
                    if (tranquilizedEntity != null) {
                      propertyAccessor.storeProperty(tranquilModel, modelProperty, tranquilizedEntity);
                    }
                  break;
                }
              } else if (isCollection(entityClass)) {
                switch (modelAnnotation.entityType()) {
                  case BASE:
                    // Simple collections are copied "as-is"
                    propertyAccessor.storeProperty(tranquilModel, modelProperty, entityPropertyValue);
                  break;
                  case COMPACT:
                    // Compact  collections are stored as lists of ids

                    List<Object> ids = new ArrayList<>();
                  	
                    for (Object collectionItem : (Collection<?>) entityPropertyValue) {
                  	Object id = propertyAccessor.extractProperty(collectionItem, "id");
                  	if (id != null)
                  	  ids.add(id);
                    }
                    
                    propertyAccessor.storeProperty(tranquilModel, modelProperty + "_ids", ids);
                  break;
                  case COMPLETE:
                    // Complete collections are stored as tranqulized entities
                    Collection<TranquilModelEntity> tranquilizedEntities = tranquilizeEntities(propertyContext, (Collection<?>) entityPropertyValue);
                    propertyAccessor.storeProperty(tranquilModel, modelProperty, tranquilizedEntities);
                  break;
                }
                
              } else if (isArray(entityClass)) {
                switch (modelAnnotation.entityType()) {
                  case BASE:
                    // Simple arrays are copied "as-is"
                    propertyAccessor.storeProperty(tranquilModel, modelProperty, entityPropertyValue);
                  break;
                  case COMPACT:
                    // Compact arrays are stored as arrays of ids
                    Object[] listItems = (Object[]) entityPropertyValue;
                    Object[] ids = new Object[listItems.length];
                  	for (int i = 0, l = listItems.length; i < l; i++) {
                      Object id = propertyAccessor.extractProperty(listItems[i], "id");
                  	  ids[i] = id;
                  	}
                    
                    propertyAccessor.storeProperty(tranquilModel, modelProperty + "_ids", ids);
                  break;
                  case COMPLETE:
                    // Complete collections are stored as tranqualized entities
                    Collection<TranquilModelEntity> tranquilizedEntities = tranquilizeEntities(propertyContext, (Collection<?>) entityPropertyValue);
                    propertyAccessor.storeProperty(tranquilModel, modelProperty, tranquilizedEntities);
                  break;
                }
              } else {
                // Simple properties are moved as is.
                propertyAccessor.storeProperty(tranquilModel, modelProperty, entityPropertyValue);
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

  private boolean isExpandedProperty(Class<?> tranquilModelClass, String modelProperty) {
    try {
      Field field = tranquilModelClass.getDeclaredField(modelProperty);
      
      return field.getAnnotation(TranquilityExpandedField.class) != null;
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    }
    
    return false;
  }

  private Object loadEntity(Object entity, Class<?> tranquilModelClass, String modelProperty) {
    try {
      Field field = tranquilModelClass.getDeclaredField(modelProperty);

      TranquilityExpandedField tranquilityEntityField = field.getAnnotation(TranquilityExpandedField.class);
      
      Class<? extends TranquilEntityResolver> entityResolverClass = tranquilityEntityField.entityResolverClass();
      
      TranquilEntityResolver entityResolver = entityResolverClass.newInstance();
      
      Object idProperty = propertyAccessor.extractProperty(entity, tranquilityEntityField.idProperty());

      if (!isCollection(field.getType())) {
        return entityResolver.resolveEntity(idProperty);
      } else {
        Collection<?> ids = (Collection<?>) idProperty;
        List<Object> items = new ArrayList<>();
        
        for (Object id : ids) {
          items.add(entityResolver.resolveEntity(id));
        }
        
        return items; 
      }
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    
    return null;
  }

  private TranquilModelEntity[] tranquilizeEntities(TranquilizingContext context, Object[] entities) {
    TranquilModelEntity[] result = new TranquilModelEntity[entities.length];
    for (int i = 0, l = entities.length; i < l; i++) {
      TranquilizingContext entityContext = new TranquilizingContext(context, entities[i].getClass(), entities[i], context.getPath());
      result[i] = tranquilizeEntity(entityContext, entities[i]);
    }

    return result;
  }

  private Collection<TranquilModelEntity> tranquilizeEntities(TranquilizingContext context, Collection<?> entities) {
    List<TranquilModelEntity> result = new ArrayList<TranquilModelEntity>();
    for (Object entity : entities) {
      TranquilizingContext entityContext = new TranquilizingContext(context, entity.getClass(), entity, context.getPath());
      TranquilModelEntity tranquilizeEntity = tranquilizeEntity(entityContext, entity);

      if (tranquilizeEntity != null)
        result.add(tranquilizeEntity);
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

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
        	classLoader = getClass().getClassLoader();
        }
        
        ProtectionDomain protectionDomain = tranquilModelClass.getProtectionDomain();
        Class<?> extendedClass = extendedCtClass.toClass(classLoader, protectionDomain);
      
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
      
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      if (contextClassLoader != null) {
      	classPool.insertClassPath(new LoaderClassPath(contextClassLoader));
      }
    }
    
    try {
			classPool.get(tranquilModelClass.getName());
		} catch (NotFoundException e) {
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
