package org.rem.scope;

import java.util.HashMap;
import java.util.Map;

public final class Environment<R, T> {
  private final Map<R, T> names = new HashMap<>();
  private final Environment<R, T> parent;
  private T lastEntry = null;

  public Environment(Environment<R, T> parent) {
    this.parent = parent;
  }

  public void put(R name, T value) {
    names.put(name, value);
    lastEntry = value;
  }

  public T get(R name) {
    T value = names.getOrDefault(name, null);
    if(value == null && parent != null) {
      return parent.get(name);
    }
    return value;
  }

  public T getLastEntry() {
    T t = this.lastEntry;
    if(t == null) return parent.getLastEntry();
    return t;
  }

  public boolean exists(R name) {
    boolean exists = names.containsKey(name);
    if(!exists && parent != null) {
      return parent.exists(name);
    }
    return exists;
  }

  public boolean existsLocal(R name) {
    return names.containsKey(name);
  }

  public Environment<R, T> getParent() {
    return parent;
  }
}
