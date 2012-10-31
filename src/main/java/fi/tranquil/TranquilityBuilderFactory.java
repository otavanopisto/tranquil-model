package fi.tranquil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import fi.tranquil.processing.PropertyAccessor;
import fi.tranquil.processing.TranquilityEntityFactory;

public class TranquilityBuilderFactory {
  
  public TranquilityBuilderFactory() {
    this(new PropertyAccessor(), 
        new TranquilityEntityFactory(
          getLookup("tranquile-entities-base.properties"), 
          getLookup("tranquile-entities-compact.properties"), 
          getLookup("tranquile-entities-complete.properties")));
  }
  
  public TranquilityBuilderFactory(PropertyAccessor propertyAccessor, TranquilityEntityFactory tranquilityEntityFactory) {
    this.propertyAccessor = propertyAccessor;
    this.tranquilityEntityFactory = tranquilityEntityFactory;
  }
  
  public TranquilityBuilder createBuilder() {
    return new TranquilityBuilder(propertyAccessor, tranquilityEntityFactory);
  }

  private static Properties getLookup(String name) {
    InputStream resourceStream = TranquilityBuilder.class.getClassLoader().getResourceAsStream(name);
    Properties lookup = new Properties();
    
    try {
      lookup.load(resourceStream);
      resourceStream.close();
    } catch (IOException e) {
    }

    return lookup;
  }
  
  private TranquilityEntityFactory tranquilityEntityFactory;
  private PropertyAccessor propertyAccessor;
}
