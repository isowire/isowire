# IsoWire - ISO8583 Framework

A comprehensive ISO8583 library for building financial transaction systems. IsoWire provides robust message processing, flexible configuration, and server/client components for payment gateway development.

## Overview

IsoWire is a lightweight, modern ISO8583 framework designed for financial applications requiring payment message processing. It simplifies the development of ATM, POS, and banking gateway systems by providing a clean, extensible architecture with YAML-based configuration.

## Features

- **Complete ISO8583 Support**: Full message parsing, formatting, and validation
- **YAML Configuration**: Easy field definitions in `iso8583-fields.yaml` with 120 pre-configured fields
- **Flexible Channels**: Pluggable channel architecture for different protocols (ASCII, Binary)
- **Server**: Virtual thread-based ISO8583 server with connection management
- **Request/Response Management**: Built-in timeout handling and correlation
- **Field Packagers**: Support for fixed-length, LLCHAR, LLLCHAR fields with subfields
- **Modern Java**: Built with Java 25 features (Records, Pattern Matching, Virtual Threads, Switch Expressions)
- **Extensible Architecture**: Easy to customize for specific payment network requirements
- **Maven Integration**: Standard dependency management and build process

- **Dynamic Bitmap Support**: The packager writes 8 or 16 byte bitmaps automatically depending on field presence; secondary-bitmap indicator is set before packing.

- **Bug fixes**: Fixed subfield unpack boundary handling and corrected example server argument parsing. The YAML top-level keys encoding/binaryEncoding/bitmapFormat were removed to avoid confusion; field config remains YAML-driven.

## Architecture

IsoWire follows a clean, modular architecture for financial transaction processing:

### Core Components

- **ISOMessage**: ISO8583 message implementation with pack/unpack functionality
- **BaseISOChannel**: Base communication channel with protocol implementations
- **ISOServer**: Server with thread pool and request handling
- **ISOFieldConfig**: YAML-driven field configuration with validation and formatting

### Channel Implementations

- **ASCIIISOChannel**: ASCII-based ISO8583 communication (4-byte length header)
- **Binary4ISOChannel**: Binary protocol support
- **TCPTransportChannel**: Raw TCP socket communication with message length encoding

### Field Configuration

Field definitions are configured in `src/main/resources/iso8583-fields.yaml`:

```yaml
fields:
  - id: 2
    name: "PAN"
    type: "com.isowire.iso.LLCHAR"
    length: 99
    description: "Primary Account Number"
```

**Variable Length Fields:**
- LLVAR fields use `length: 99` (2-digit length indicator, max 99 characters)
- LLLVAR fields use `length: 999` (3-digit length indicator, max 999 characters)

**Implicit Padding Rules:**
- **Numeric fields** (NUMERIC, AMOUNT): Left-padded with zeros
- **Alphanumeric fields** (CHAR): Right-padded with spaces
- **Variable fields** (LLCHAR, LLLCHAR): No padding (length prefix only)

## Quick Start

### Prerequisites

- Java 25 (GraalVM CE 25.0.2 recommended)
- Maven wrapper included (no Maven installation required)
- OR Maven 3.9+ (if using Maven directly)

### Building

**Using Maven Wrapper (Recommended):**
```bash
# Compile the project
./mvnw clean compile

# Package as JAR
./mvnw clean package
```

**Using Shell Scripts:**
```bash
# Compile the project
./compile.sh
```

**Using Maven (if installed):**
```bash
mvn clean compile
mvn clean package
```

### Running the Server

```bash
# Start server on default port 9999
./start-server.sh

# Start server on custom port
./start-server.sh 8888

# Start server with custom port and thread pool
./start-server.sh 8888 20
```

### Running the Test Client

```bash
# Test against localhost:9999
./run-test-client.sh

# Test against specific host:port
./run-test-client.sh localhost 8888
```

## Usage Examples

### Creating a Payment Server

```java
import com.isowire.iso.ISOMessage;
import com.isowire.iso.ISOServer;
import com.isowire.iso.ISORequestListener;

public class PaymentServer {
    public static void main(String[] args) throws IOException {
        ISOServer server = new ISOServer(9999, 50);
        server.setRequestListener(new PaymentRequestListener());
        server.start();
    }
}

class PaymentRequestListener implements ISORequestListener {
    @Override
    public ISOMessage process(ISOMessage request) {
        // Process payment logic
        ISOMessage response = new ISOMessage();
        response.setMTI(request.getMTI().substring(0, 2) + "10");

        // Copy essential fields
        if (request.hasField(2)) response.set(2, request.get(2));
        if (request.hasField(4)) response.set(4, request.get(4));

        // Add response code
        response.set(39, "00"); // Approved

        return response;
    }
}
```

### Creating a Payment Client

```java
import com.isowire.iso.ISOMessage;
import com.isowire.iso.channel.ASCII4ISOChannel;
import com.isowire.iso.packager.ISODefaultPackager;
import com.isowire.iso.channel.TCPTransportChannel;
import packager.com.isowire.iso.ISOException;

public class PaymentClient {
    public static void main(String[] args) throws ISOException {
        ASCII4ISOChannel channel = new ASCII4ISOChannel(new ISODefaultPackager(), new TCPTransportChannel("payment-gateway.com", 9999));
        channel.connect();

        // Create financial transaction request
        ISOMessage request = createPaymentRequest(
                "1234567890123456",  // PAN
                "000000001000",      // Amount
                "TERM001"            // Terminal ID
        );

        // Send and receive response
        ISOMessage response = channel.sendAndWait(request, 30000);

        System.out.println("Response Code: " + response.get(39));

        channel.disconnect();
    }

    private static ISOMessage createPaymentRequest(String pan, String amount, String terminalId) {
        ISOMessage msg = new ISOMessage();
        msg.setMTI("0100");  // Authorization request
        msg.set(2, pan);
        msg.set(4, amount);
        msg.set(11, generateStan());
        msg.set(41, terminalId);
        return msg;
    }
}
```

## Field Types

### Supported Field Types

- **NUMERIC** (com.isowire.iso.NUMERIC): Fixed-length numeric fields (auto left-zero padded)
- **CHAR** (com.isowire.iso.CHAR): Fixed-length alphanumeric fields (auto right-space padded)
- **LLCHAR** (com.isowire.iso.LLCHAR): Variable-length fields with 2-digit length indicator (max 99 characters)
- **LLLCHAR** (com.isowire.iso.LLLCHAR): Variable-length fields with 3-digit length indicator (max 999 characters)
- **AMOUNT** (com.isowire.iso.AMOUNT): Fixed-length amount fields (typically 12 digits, auto left-zero padded)
- **ASCII_BITMAP** (com.isowire.iso.ASCII_BITMAP): ASCII-formatted bitmap handling (automatic)
- **BITMAP** (com.isowire.iso.BITMAP): Binary bitmap packager (8 or 16 bytes binary)

Note: Subfields are supported via the `subFields` configuration. Subfields are represented as nested ISOFieldConfig entries and use the same packager classes (e.g., NUMERIC, CHAR, LLCHAR). For custom encodings, implement an ISOFieldPackager and set the field's `type` to the packager's fully-qualified class name.

### Automatic Padding

The framework applies ISO8583 standard padding automatically - no manual configuration needed:

**Numeric Fields (NUMERIC, AMOUNT):**
```java
msg.set(4, "1000");    // Amount field (length 12)
// On wire: "000000001000" (left-padded with zeros)
```

**Alphanumeric Fields (CHAR):**
```java
msg.set(41, "TERM01");  // Terminal ID (length 8)  
// On wire: "TERM01  " (right-padded with spaces)
```

**Variable Fields (LLCHAR, LLLCHAR):**
```java
msg.set(2, "1234567890123456"); // PAN (LLCHAR, max 99)
// On wire: "161234567890123456" (2-digit length + data, no padding)

msg.set(48, "TAG1VALUE1TAG2VALUE2"); // LLCHAR with subfields (see field configuration)
// On wire: length-prefixed data per LLCHAR packager
```

**Dynamic Length Validation:**
The packager automatically calculates maximum length based on length indicator:
- LLVAR (2 digits) → max 99 characters
- LLLVAR (3 digits) → max 999 characters

### Field Configuration Options

Each field supports:
- **Implicit Padding**: Automatic based on field type (numeric: left-zero, alphanumeric: right-space)
- **Length Validation**: Dynamic max length calculation based on field type
- **Formatting**: Numeric, alphanumeric, binary encoding
- **Subfields**: Composite field support via nested field configurations (e.g., field 48, 62, 63)

## Message Format

IsoWire uses standard ISO8583 message format:

```
[Length Header: 4 bytes][MTI: 4 bytes][Bitmap: 8/16 bytes][Data Fields]
```

**Example message structure:**
1. **Length Header**: 4 bytes ASCII (big-endian) indicating message length
2. **MTI**: 4 bytes ASCII (e.g., "0100" for authorization request)
3. **Bitmap**: 8 or 16 bytes indicating which fields are present
4. **Data Fields**: Variable-length fields based on bitmap

## Customization

### Adding Custom Field Types

```java
public class EPOCH extends NUMERIC {
    @Override
    public String pack(DataOutputStream dos, ISOFieldConfig config, String value) 
            throws IOException, ISOException {
        // Convert timestamp to epoch seconds
        long epoch = System.currentTimeMillis() / 1000;
        return super.pack(dos, config, String.valueOf(epoch));
    }
}
```

### Creating Custom Channels

```java
public class SSLChannel extends BaseISOChannel {
    private SSLContext sslContext;
    
    @Override
    public void connect() throws ISOException {
        // Implement SSL/TLS connection
        SSLSocketFactory factory = sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
        // Configure socket and set in channel
    }
}
```

### Extended Configuration

Add custom fields to `iso8583-fields.yaml`:

```yaml
- id: 60
  name: "Reserved Private"
  type: "com.isowire.iso.LLLCHAR"
  length: 999
  description: "Reserved - Private Use"
  subFields:
    - id: 1
      name: "Auth Type"
      type: "com.isowire.iso.CHAR"
      length: 2
      description: "Authentication type indicator"
```

**Subfield Configuration:**
```yaml
- id: 48
  name: "Additional Data"
  type: "com.isowire.iso.LLCHAR"
  length: 999
  description: "Additional data - Private use"
  subFields:
    - id: 0x01
      name: "Subfield_1"
      type: "com.isowire.iso.LLCHAR"
      length: 999
      description: "Custom subfield (use a custom packager for special encodings)"
```

## Building

**Using Maven Wrapper (Recommended):**
```bash
# Compile the project
./mvnw clean compile

# Package as JAR
./mvnw clean package
```

**Using Shell Scripts:**
```bash
# Compile the project
./compile.sh
```

**Using Maven (if installed):**
```bash
mvn clean compile
mvn clean package
```

## Integration

### Maven Dependency

```xml
<dependency>
    <groupId>com.isowire</groupId>
    <artifactId>mpos</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Spring Boot Integration

```java
@Configuration
public class IsoWireConfig {
    
    @Bean
    public ISOServer isoServer(ISORequestListener requestListener) {
        ISOServer server = new ISOServer(9999, 50);
        server.setRequestListener(requestListener);
        return server;
    }
    
    @Bean
    public ASCIIISOChannel isoChannel() {
        return new ASCIIISOChannel("payment-host", 9999);
    }
}
```

## Logging

IsoWire uses SLF4J with Logback for flexible logging:

- **Console**: Real-time logging during development
- **File**: `logs/mpos-server.log` with automatic rotation
- **Configurable**: Log levels and formats in `logback.xml`

## Performance

- **Virtual Threads**: Java 25 virtual threads for highly concurrent request handling
- **Efficient I/O**: Non-blocking message processing with TCP transport channels
- **Dynamic Validation**: Runtime length validation without hardcoded checks
- **Modern Architecture**: Records for immutable configuration, pattern matching for clean code

## Use Cases

IsoWire is ideal for:
- **Payment Gateways**: Process authorization and settlement messages
- **ATM Systems**: Handle cash withdrawal and balance inquiries
- **POS Terminals**: Manage card-present transactions
- **Banking Interfaces**: Connect to switches and networks
- **Testing Tools**: ISO8583 message testing and simulation

## Support

For detailed usage examples and Maven commands, see:
- `QUICKSTART.md` - Quick start guide
- `MAVEN_COMMANDS.md` - Maven build commands

## License

This project is provided as-is for financial application development.

## Contributing

IsoWire is designed for extensibility. Common customizations include:
- Additional field packagers for specific network requirements
- Custom channel implementations for secure communication
- Extended validation rules for compliance
- Integration with specific payment network protocols
