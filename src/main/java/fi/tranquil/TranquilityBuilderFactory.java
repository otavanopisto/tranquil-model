package fi.tranquil;

import fi.tranquil.processing.EntityLookup;
import fi.tranquil.processing.PropertyAccessor;
import fi.tranquil.processing.TranquilityEntityFactory;

public class TranquilityBuilderFactory {
  
  public TranquilityBuilderFactory() {
    this(new PropertyAccessor(), 
        new TranquilityEntityFactory(
          getLookup("fi.tranquil.BaseLookup"), 
          getLookup("fi.tranquil.CompactLookup"), 
          getLookup("fi.tranquil.CompleteLookup")));
  }
  
  public TranquilityBuilderFactory(PropertyAccessor propertyAccessor, TranquilityEntityFactory tranquilityEntityFactory) {
    this.propertyAccessor = propertyAccessor;
    this.tranquilityEntityFactory = tranquilityEntityFactory;
  }
  
  public TranquilityBuilder createBuilder() {
    return new TranquilityBuilder(propertyAccessor, tranquilityEntityFactory);
  }

  private static EntityLookup getLookup(String name) {
    try {
      return (EntityLookup) TranquilityBuilder.class.getClassLoader().loadClass(name).newInstance();
    } catch (Exception e) {
      return null;
    }
  }
  
  private TranquilityEntityFactory tranquilityEntityFactory;
  private PropertyAccessor propertyAccessor;
}
