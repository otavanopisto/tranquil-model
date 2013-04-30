package fi.tranquil;

public interface TranquilEntityResolver {
  Object resolveEntity(Object entity, String idProperty);
}
