package fi.tranquil;

public class TranquilizingContext {

  public TranquilizingContext(TranquilizingContext parentContext, Class<?> entityClass, Object entityValue, String path) {
    this.parentContext = parentContext;
    this.entityValue = entityValue;
    this.entityClass = entityClass;
    this.path = path;
  }
  
  public TranquilizingContext(TranquilizingContext parentContext, Class<?> entityClass, String path) {
    this(parentContext, entityClass, null, path);
  }
  
  public TranquilizingContext(Class<?> entityClass, Object entityValue, String path) {
    this(null, entityClass, entityValue, path);
  }

  public TranquilizingContext(Class<?> entityClass, String path) {
    this(null, entityClass, null, path);
  }
  
  public Class<?> getEntityClass() {
    return entityClass;
  }
  
  public void setEntityClass(Class<?> entityClass) {
    this.entityClass = entityClass;
  }
  
  public Object getEntityValue() {
    return entityValue;
  }
  
  public void setEntityValue(Object entityValue) {
    this.entityValue = entityValue;
  }
  
  public String getPath() {
    return path;
  }
  
  public void setPath(String path) {
    this.path = path;
  }
  
  public TranquilizingContext getParentContext() {
    return parentContext;
  }
  
  private String path;
  private Class<?> entityClass;
  private Object entityValue;
  private TranquilizingContext parentContext;
}
