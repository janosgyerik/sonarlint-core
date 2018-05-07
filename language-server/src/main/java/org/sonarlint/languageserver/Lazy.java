package org.sonarlint.languageserver;

import java.util.function.Supplier;

public class Lazy<T> {

  private final Supplier<T> supplier;
  private T instance;

  public Lazy(Supplier<T> supplier) {
    this.supplier = supplier;
  }

  public T get() {
    if (instance == null) {
      instance = supplier.get();
    }
    return instance;
  }
}
