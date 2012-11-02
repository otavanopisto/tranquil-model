package fi.tranquil.impl;

public class TranquilizingContext {

  public TranquilizingContext(Class<?> entityClass, String path) {
    this.entityClass = entityClass;
    this.path = path;
  }
  
  public Class<?> getEntityClass() {
    return entityClass;
  }
  
  public void setEntityClass(Class<?> entityClass) {
    this.entityClass = entityClass;
  }
  
  public String getPath() {
    return path;
  }
  
  public void setPath(String path) {
    this.path = path;
  }
  
  private String path;
  private Class<?> entityClass;
}
