package fi.tranquil.processing;

import fi.tranquil.TranquilModelType;

public class TranquilityEntityFactory {

  public TranquilityEntityFactory(EntityLookup baseLookup, EntityLookup compactLookup, EntityLookup completeLookup) {
    this.baseLookup = baseLookup;
    this.compactLookup = compactLookup;
    this.completeLookup = completeLookup;
  }
  
  public Class<?> findTranquilModel(Class<?> entity, TranquilModelType type) {
    Class<?> tranquilModel = getLookup(type).findTranquilModel(entity);
    if (tranquilModel != null)
			return tranquilModel;
    
  	Class<?>[] interfaces = entity.getInterfaces();
  	for (Class<?> intf : interfaces) {
  		tranquilModel = findTranquilModel(intf, type);
  		if (tranquilModel != null)
  			return tranquilModel;
  	}
  	
  	Class<?> superclass = entity.getSuperclass();
  	if (superclass != null && !Object.class.equals(superclass)) {
  		return findTranquilModel(superclass, type);
  	}
  	
  	return null;
  }
  
  private EntityLookup getLookup(TranquilModelType type) {
    switch (type) {
      case BASE:
        return baseLookup;
      case COMPACT:
        return compactLookup;
      case COMPLETE:
        return completeLookup;
    }
    
    return null;
  }
  
  private EntityLookup baseLookup;
  private EntityLookup compactLookup;
  private EntityLookup completeLookup;
}