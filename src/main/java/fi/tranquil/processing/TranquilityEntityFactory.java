package fi.tranquil.processing;

import fi.tranquil.TranquilModelType;

public class TranquilityEntityFactory {

  public TranquilityEntityFactory(EntityLookup baseLookup, EntityLookup compactLookup, EntityLookup completeLookup) {
    this.baseLookup = baseLookup;
    this.compactLookup = compactLookup;
    this.completeLookup = completeLookup;
  }
  
  public Class<?> findTranquileModel(Class<?> entity, TranquilModelType type) {
    return getLookup(type).findTranquileModel(entity);
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