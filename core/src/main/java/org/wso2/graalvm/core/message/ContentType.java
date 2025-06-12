package org.wso2.graalvm.core.message;

/**
 * Represents content types with MIME type support and common constants.
 */
public final class ContentType {
    
    // Common content types
    public static final ContentType APPLICATION_JSON = new ContentType("application", "json");
    public static final ContentType APPLICATION_XML = new ContentType("application", "xml");
    public static final ContentType TEXT_XML = new ContentType("text", "xml");
    public static final ContentType TEXT_PLAIN = new ContentType("text", "plain");
    public static final ContentType TEXT_HTML = new ContentType("text", "html");
    public static final ContentType TEXT_CSV = new ContentType("text", "csv");
    public static final ContentType APPLICATION_YAML = new ContentType("application", "yaml");
    public static final ContentType APPLICATION_FORM_URLENCODED = new ContentType("application", "x-www-form-urlencoded");
    public static final ContentType MULTIPART_FORM_DATA = new ContentType("multipart", "form-data");
    public static final ContentType APPLICATION_OCTET_STREAM = new ContentType("application", "octet-stream");
    public static final ContentType IMAGE_JPEG = new ContentType("image", "jpeg");
    public static final ContentType IMAGE_PNG = new ContentType("image", "png");
    public static final ContentType APPLICATION_PDF = new ContentType("application", "pdf");
    
    private final String type;
    private final String subtype;
    private final String charset;
    private final String boundary;
    
    public ContentType(String type, String subtype) {
        this(type, subtype, null, null);
    }
    
    public ContentType(String type, String subtype, String charset) {
        this(type, subtype, charset, null);
    }
    
    public ContentType(String type, String subtype, String charset, String boundary) {
        this.type = type.toLowerCase();
        this.subtype = subtype.toLowerCase();
        this.charset = charset;
        this.boundary = boundary;
    }
    
    /**
     * Parse a content type string (e.g., "application/json; charset=utf-8").
     */
    public static ContentType parse(String contentTypeString) {
        if (contentTypeString == null || contentTypeString.trim().isEmpty()) {
            return APPLICATION_OCTET_STREAM;
        }
        
        String[] parts = contentTypeString.split(";");
        String[] typeParts = parts[0].trim().split("/");
        
        if (typeParts.length != 2) {
            throw new IllegalArgumentException("Invalid content type: " + contentTypeString);
        }
        
        String type = typeParts[0].trim();
        String subtype = typeParts[1].trim();
        String charset = null;
        String boundary = null;
        
        // Parse parameters
        for (int i = 1; i < parts.length; i++) {
            String param = parts[i].trim();
            if (param.startsWith("charset=")) {
                charset = param.substring(8).trim();
                if (charset.startsWith("\"") && charset.endsWith("\"")) {
                    charset = charset.substring(1, charset.length() - 1);
                }
            } else if (param.startsWith("boundary=")) {
                boundary = param.substring(9).trim();
                if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                    boundary = boundary.substring(1, boundary.length() - 1);
                }
            }
        }
        
        return new ContentType(type, subtype, charset, boundary);
    }
    
    public String getType() {
        return type;
    }
    
    public String getSubtype() {
        return subtype;
    }
    
    public String getCharset() {
        return charset;
    }
    
    public String getBoundary() {
        return boundary;
    }
    
    public String getMimeType() {
        return type + "/" + subtype;
    }
    
    public boolean isJson() {
        return "application".equals(type) && "json".equals(subtype);
    }
    
    public boolean isXml() {
        return ("application".equals(type) && "xml".equals(subtype)) ||
               ("text".equals(type) && "xml".equals(subtype));
    }
    
    public boolean isText() {
        return "text".equals(type);
    }
    
    public boolean isBinary() {
        return !isText() && !isJson() && !isXml();
    }
    
    public boolean isMultipart() {
        return "multipart".equals(type);
    }
    
    /**
     * Check if this content type is compatible with another.
     * Compatible means they can be processed by the same handlers.
     */
    public boolean isCompatibleWith(ContentType other) {
        if (this.equals(other)) {
            return true;
        }
        
        // JSON variants
        if ((this.isJson() && other.isJson()) ||
            (this.getMimeType().endsWith("+json") && other.isJson()) ||
            (this.isJson() && other.getMimeType().endsWith("+json"))) {
            return true;
        }
        
        // XML variants
        if ((this.isXml() && other.isXml()) ||
            (this.getMimeType().endsWith("+xml") && other.isXml()) ||
            (this.isXml() && other.getMimeType().endsWith("+xml"))) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Create a new ContentType with different charset.
     */
    public ContentType withCharset(String charset) {
        return new ContentType(this.type, this.subtype, charset, this.boundary);
    }
    
    /**
     * Create a new ContentType with boundary (for multipart).
     */
    public ContentType withBoundary(String boundary) {
        return new ContentType(this.type, this.subtype, this.charset, boundary);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append("/").append(subtype);
        
        if (charset != null) {
            sb.append("; charset=").append(charset);
        }
        
        if (boundary != null) {
            sb.append("; boundary=").append(boundary);
        }
        
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ContentType that = (ContentType) obj;
        return type.equals(that.type) && 
               subtype.equals(that.subtype) &&
               java.util.Objects.equals(charset, that.charset) &&
               java.util.Objects.equals(boundary, that.boundary);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, subtype, charset, boundary);
    }
}
