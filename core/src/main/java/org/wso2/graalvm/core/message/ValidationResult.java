package org.wso2.graalvm.core.message;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Result of message validation operations.
 */
public class ValidationResult {
    
    private final boolean valid;
    private final List<ValidationError> errors;
    private final List<ValidationWarning> warnings;
    
    private ValidationResult(boolean valid, List<ValidationError> errors, List<ValidationWarning> warnings) {
        this.valid = valid;
        this.errors = Collections.unmodifiableList(errors);
        this.warnings = Collections.unmodifiableList(warnings);
    }
    
    public static ValidationResult valid() {
        return new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
    }
    
    public static ValidationResult invalid(List<ValidationError> errors) {
        return new ValidationResult(false, errors, Collections.emptyList());
    }
    
    public static ValidationResult invalid(String error) {
        return invalid(List.of(new ValidationError("VALIDATION_FAILED", error, null)));
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public List<ValidationError> getErrors() {
        return errors;
    }
    
    public List<ValidationWarning> getWarnings() {
        return warnings;
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    public static class Builder {
        private final List<ValidationError> errors = new ArrayList<>();
        private final List<ValidationWarning> warnings = new ArrayList<>();
        
        public Builder addError(String code, String message, String path) {
            errors.add(new ValidationError(code, message, path));
            return this;
        }
        
        public Builder addError(String message) {
            return addError("VALIDATION_ERROR", message, null);
        }
        
        public Builder addWarning(String code, String message, String path) {
            warnings.add(new ValidationWarning(code, message, path));
            return this;
        }
        
        public Builder addWarning(String message) {
            return addWarning("VALIDATION_WARNING", message, null);
        }
        
        public Builder merge(ValidationResult other) {
            this.errors.addAll(other.getErrors());
            this.warnings.addAll(other.getWarnings());
            return this;
        }
        
        public ValidationResult build() {
            return new ValidationResult(errors.isEmpty(), errors, warnings);
        }
    }
    
    public static class ValidationError {
        private final String code;
        private final String message;
        private final String path;
        
        public ValidationError(String code, String message, String path) {
            this.code = code;
            this.message = message;
            this.path = path;
        }
        
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public String getPath() { return path; }
        
        @Override
        public String toString() {
            return String.format("ValidationError{code='%s', message='%s', path='%s'}", code, message, path);
        }
    }
    
    public static class ValidationWarning {
        private final String code;
        private final String message;
        private final String path;
        
        public ValidationWarning(String code, String message, String path) {
            this.code = code;
            this.message = message;
            this.path = path;
        }
        
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public String getPath() { return path; }
        
        @Override
        public String toString() {
            return String.format("ValidationWarning{code='%s', message='%s', path='%s'}", code, message, path);
        }
    }
}
