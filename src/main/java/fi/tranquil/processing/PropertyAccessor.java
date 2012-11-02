package fi.tranquil.processing;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;

public class PropertyAccessor {
  
  public void storeProperty(Object object, String property, Object value) {
    storeProperty(object, property, value, true);
  }
  
  public void storeProperty(Object object, String property, Object value, boolean preferSetter) {
    try {
      Method setterMethod = preferSetter ? findSetter(object.getClass(), property, value.getClass()) : null;
      if (setterMethod != null) {
        setterMethod.invoke(object, value);
      } else {
        Field field = getField(object.getClass(), property);
        field.setAccessible(true);
        field.set(object, value);
      }
    } catch (Exception e) { 
      e.printStackTrace();
    }
  }
  
  private Method findSetter(Class<?> objectClass, String property, Class<?> valueClass) {
    Method setterMethod = getMethod(objectClass, "set" + StringUtils.capitalize(property), valueClass);
    if (setterMethod == null && valueClass.getSuperclass() != null && !valueClass.getSuperclass().equals(Object.class)) {
      return findSetter(objectClass, property, valueClass.getSuperclass());
    }
    
    if (setterMethod == null && valueClass.getInterfaces() != null) {
      for (Class<?> interfaceClass : valueClass.getInterfaces()) {
        setterMethod = findSetter(objectClass, property, interfaceClass);
        if (setterMethod != null)
          break;
      }
    }
    
    return setterMethod;
  }
  
  public Object extractProperty(Object object, String property) {
    return extractProperty(object, property, true);
  }
  
  public Object extractProperty(Object object, String property, boolean preferGetter) {
    try {
      Method getterMethod = preferGetter ? getMethod(object.getClass(), "get" + StringUtils.capitalize(property)) : null;
      if (getterMethod != null) {
        return getterMethod.invoke(object);
      } else {
        Field field = getField(object.getClass(), property);
        if (field != null) {
          field.setAccessible(true);
          return field.get(object);
        } else {
          return null;
        }
      }
    } catch (Exception e) {
      return null;
    }
  }
  
  public Class<?> getFieldType(Class<?> entityClass, String property) {
    Method getterMethod = getMethod(entityClass, "get" + StringUtils.capitalize(property));
    if (getterMethod != null) {
      return getterMethod.getReturnType();
    }
    
    Field field = getField(entityClass, property);
    if (field != null) 
      return field.getType();
    
    return null;
  }

  private Method getMethod(Class<?> entityClass, String name, Class<?>... parameterTypes) {
    try {
      return entityClass.getDeclaredMethod(name, parameterTypes);
    } catch (SecurityException e) {
      return null;
    } catch (NoSuchMethodException e) {
      Class<?> superClass = entityClass.getSuperclass();
      if (superClass != null && !Object.class.equals(superClass))
        return getMethod(superClass, name);
    }

    return null;
  }

  private Field getField(Class<?> entityClass, String name) {
    try {
      return entityClass.getDeclaredField(name);
    } catch (SecurityException e) {
      return null;
    } catch (NoSuchFieldException e) {
      Class<?> superClass = entityClass.getSuperclass();
      if (superClass != null && !Object.class.equals(superClass))
        return getField(superClass, name);
    }

    return null;
  }

}
