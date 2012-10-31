package fi.tranquil.processing;

import java.util.Properties;

import fi.tranquil.TranquilModelType;

public class TranquilityEntityFactory {

  public TranquilityEntityFactory(Properties baseLookup, Properties compactLookup, Properties completeLookup) {
    this.baseLookup = baseLookup;
    this.compactLookup = compactLookup;
    this.completeLookup = completeLookup;
  }
  
  public Class<?> findTranquileModel(Class<?> entity, TranquilModelType type) {
    String tranquileClassName = (String) getLookup(type).get(entity.getName());
    if (tranquileClassName != null) {
      try {
        return Class.forName(tranquileClassName);
      } catch (ClassNotFoundException e) {
        return null;
      }
    }

    return null;
  }
  
  private Properties getLookup(TranquilModelType type) {
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
  
  private Properties baseLookup;
  private Properties compactLookup;
  private Properties completeLookup;
}