package org.rem.exceptions;

public class AnalysisException extends RuntimeException {
  public AnalysisException(String message) {
    super("\n" + message);
  }
}
