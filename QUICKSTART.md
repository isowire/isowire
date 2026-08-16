# IsoWire - Quick Start Guide

## Project Overview

IsoWire is a comprehensive ISO8583 framework for building financial transaction systems. It provides robust message processing, flexible configuration, and components for payment gateway development.

## Components

- **ISOMessage**: ISO8583 message implementation with pack/unpack
- **BaseISOChannel**: Communication channels (ASCII, Binary protocols)
- **ISOServer**: ISO8583 server with virtual threads
- **YAML Configuration**: Easy field configuration with 120+ pre-defined fields
- **Field Packagers**: Support for fixed-length, LLCHAR, LLLCHAR fields with subfields
- **Modern Java**: Built with Java 25 features (Records, Pattern Matching, Virtual Threads)

## Quick Start

### Prerequisites

- Java 25 (GraalVM CE 25.0.2 recommended)
- Maven wrapper included (no Maven installation required)

### 1. Start the Server

```bash
# Usage: start-server.sh [port] [channel]
# Default: port 9999, channel ASCII4
./start-server.sh

# Start on custom port with default channel
./start-server.sh 8888

# Start with custom port and channel (e.g., ASCII4 or BINARY2)
./start-server.sh 8888 ASCII4
```

### 2. Run Test Client

```bash
./run-test-client.sh [host] [port]

# Default: localhost:9999
./run-test-client.sh

# Custom: localhost:8888
./run-test-client.sh localhost 8888
```

## Architecture

### Package Structure

```
example/                         # Example applications
├── SampleIsoWireServerLauncher           # Server entry point
└── SampleIsoWireClient               # Test client

com.isowire.iso/            # Core ISO8583 framework
├── ISOMessage                      # ISO8583 message
├── ISOFieldConfig                 # Field configuration
├── ISOServer                     # Server implementation
├── ISORequestListener         # Request processing interface
├── packagers/                  # Field packagers
│   ├── ISOBasePackager
│   ├── FixedFieldPackager
│   ├── ISOVarFieldPackager       # LLVAR/LLLVAR support
│   └── ISO87BinaryPackager
└── channels/                   # Channel implementations
    ├── BaseISOChannel
    ├── ASCIIISOChannel
    ├── Binary4ISOChannel
    ├── TCPTransportChannel
    └── ISOMessageLength encoders
```

### YAML Field Configuration

Field definitions in `iso8583-fields.yaml`:

```yaml
fields:
  - id: 2
    name: "PAN"
    type: "com.isowire.iso.LLCHAR"
    length: 99
    description: "Primary Account Number"
```

**Variable Length Field Configuration:**
- LLCHAR fields: `length: 99` (2-digit length indicator, max 99 characters)
- LLLCHAR fields: `length: 999` (3-digit length indicator, max 999 characters)

**Implicit Padding is Applied Automatically:**
- **Numeric fields** (NUMERIC, AMOUNT): `"123"` → `"000123"` (left-zero padded)
- **Alphanumeric fields** (CHAR): `"ABC"` → `"ABC   "` (right-space padded)
- **Variable fields** (LLCHAR, LLLCHAR): No padding (length prefix only)

## Usage Examples

### Creating a Custom Request Listener

```java
import com.isowire.iso.ISOMessage;
import com.isowire.iso.ISORequestListener;

public class PaymentProcessor implements ISORequestListener {
   @Override
   public ISOMessage process(ISOMessage request) {
      ISOMessage response = new ISOMessage();
      response.setMTI(request.getMTI().substring(0, 2) + "10");

      // Copy essential fields
      if (request.hasField(2)) response.set(2, request.get(2));
      if (request.hasField(3)) response.set(3, request.get(3));
      if (request.hasField(4)) response.set(4, request.get(4));

      // Set response code
      response.set(39, "00"); // Approved

      return response;
   }
}
```

### Using ASCII Channel for Client Communication

```java
import com.isowire.iso.ISOMessage;
import com.isowire.iso.channel.ASCII4ISOChannel;
import com.isowire.iso.packager.ISODefaultPackager;
import com.isowire.iso.channel.TCPTransportChannel;

// Create channel
ASCII4ISOChannel channel = new ASCII4ISOChannel(new ISODefaultPackager(), new TCPTransportChannel("payment-host", 9999));

// Connect
channel.

        connect();

        // Create request
        ISOMessage request = new ISOMessage();
request.

        setMTI("0100");           // Authorization Request
request.

        set(2,"1234567890123456"); // PAN
request.

        set(4,"000000001000");    // Amount ($10.00)
request.

        set(11,"000001");         // STAN

        // Send and receive response
        ISOMessage response = channel.sendAndWait(request, 30000);

System.out.

        println("Response: "+response.get(39)); // Approval code

// Disconnect
        channel.

        disconnect();
```

### Creating ISO8583 Messages

```java
ISOMessage msg = new ISOMessage();
msg.setMTI("0100");              // Authorization Request
msg.set(2, "1234567890123456");  // Primary Account Number
msg.set(3, "000000");             // Processing Code
msg.set(4, "000000001000");       // Transaction Amount
msg.set(11, "000001");            // System Trace Audit Number
msg.set(12, "123456");            // Local Transaction Time
msg.set(13, "0802");              // Local Transaction Date
msg.set(41, "TERM001");           // Terminal ID
msg.set(42, "MERCHANT01");        // Merchant ID
msg.set(49, "840");               // Currency Code (USD)
```

## Common MTIs

- `0100`: Authorization Request
- `0110`: Authorization Response
- `0200`: Financial Transaction Request
- `0210`: Financial Transaction Response
- `0400`: Reversal Request
- `0410`: Reversal Response
- `0800`: Network Management Request
- `0810`: Network Management Response

## Common Response Codes

- `00`: Approved or processed successfully
- `01`: Refer to card issuer
- `03`: Invalid merchant
- `05`: Do not honor
- `12`: Invalid transaction
- `13`: Invalid amount
- `14`: Invalid card number
- `51`: Insufficient funds
- `54`: Expired card
- `55`: Incorrect PIN

## Field Configuration

### Field Types

- **NUMERIC** (com.isowire.iso.NUMERIC): Fixed-length numeric fields (auto left-zero padded)
- **CHAR** (com.isowire.iso.CHAR): Fixed-length alphanumeric fields (auto right-space padded)
- **LLCHAR** (com.isowire.iso.LLCHAR): Variable-length fields with 2-digit length indicator (max 99 characters)
- **LLLCHAR** (com.isowire.iso.LLLCHAR): Variable-length fields with 3-digit length indicator (max 999 characters)
- **AMOUNT** (com.isowire.iso.AMOUNT): Fixed-length amount fields (typically 12 digits, auto left-zero padded)
- **ASCII_BITMAP** (com.isowire.iso.ASCII_BITMAP): ASCII-formatted bitmap handling (automatic)
- **BITMAP** (com.isowire.iso.BITMAP): Binary bitmap packager (8 or 16 bytes binary)

Note: Subfields are supported via the `subFields` configuration. Subfields are represented as nested ISOFieldConfig entries and use the same packager classes (e.g., NUMERIC, CHAR, LLCHAR). For custom encodings, implement an ISOFieldPackager and set the field's `type` to the packager's fully-qualified class name.

### Automatic Padding Behavior

The framework applies padding automatically based on field type:

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
```

### Field Configuration Options

```yaml
- id: 2
  name: "PAN"
  type: "com.isowire.iso.LLCHAR"
  length: 99
  description: "Primary Account Number"
```

**Subfield Configuration Example:**
```yaml
- id: 48
  name: "Additional Data"
  type: "com.isowire.iso.LLCHAR"
  length: 999
  description: "Additional data - Private use"
  subFields:
    - id: 1
      name: "Subfield_1"
      type: "com.isowire.iso.LLCHAR"
      length: 999
      description: "Custom subfield"
```

**Note:** Padding is applied automatically based on field type - no explicit padding configuration needed.

## Customization

### Add Custom Fields

Edit `src/main/resources/iso8583-fields.yaml`:

```yaml
- id: 60
  name: private_field_60
  type: com.isowire.iso.LLLCHAR
  length: 999
  description: Private Field - Network Specific
```

### Create Custom Channel

```java
import channel.com.isowire.iso.BaseISOChannel;
import packager.com.isowire.iso.ISOException;

public class SSLChannel extends BaseISOChannel {

   @Override
   public void connect() throws ISOException {
      // Implement SSL/TLS connection logic
      try {
         SSLSocketFactory factory = SSLContext.getDefault().getSSLSocketFactory();
         SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
         // Configure and set socket
      } catch (Exception e) {
         throw new ISOException("SSL connection failed", e);
      }
   }
}
```

### Custom Field Packager

```java
import packager.com.isowire.iso.ISOFieldPackager;

public class ENCRYPTED extends LLCHAR {
    
    @Override
    public void pack(DataOutputStream dos, ISOFieldConfig config, String value) 
            throws IOException, ISOException {
        // Encrypt value before packing
        String encrypted = encrypt(value);
        super.pack(dos, config, encrypted);
    }
    
    @Override
    public String unpack(ByteBuffer buffer, ISOFieldConfig config) 
            throws ISOException {
        String encrypted = super.unpack(buffer, config);
        return decrypt(encrypted);
    }
}
```

## Message Format

IsoWire uses standard ISO8583 message format:

```
┌─────────────┬─────────┬─────────┬─────────────┐
│ Length (4B) │ MTI (4) │ Bitmap  │  Data Fields │
│   ASCII     │  ASCII  │  8/16B  │  Variable   │
└─────────────┴─────────┴─────────┴─────────────┘
```

**Component breakdown:**
1. **Length Header**: 4 bytes ASCII (big-endian) indicating total message length
2. **MTI**: 4 bytes ASCII (e.g., "0100" for authorization request)
3. **Bitmap**: 8 or 16 bytes indicating which fields are present
4. **Data Fields**: Variable-length fields based on bitmap presence

## Building

### Using Maven Wrapper (Recommended)

```bash
# Clean compile
./mvnw clean compile

# Package
./mvnw clean package

# Run server
./mvnw exec:java -Dexec.mainClass="com.isowire.example.SampleIsoWireServer" -Dexec.args="9999 10"

# Run client
./mvnw exec:java -Dexec.mainClass="com.isowire.example.SampleIsoWireClient" -Dexec.args="localhost 9999"
```

### Using Shell Scripts

```bash
# Compile
./compile.sh

# Start server
./start-server.sh

# Run client
./run-test-client.sh
```

### Using Maven (if installed)

```bash
# Clean compile
mvn clean compile

# Package
mvn clean package

# Run server
mvn exec:java -Dexec.mainClass="com.isowire.example.SampleIsoWireServer" -Dexec.args="9999 10"

# Run client
mvn exec:java -Dexec.mainClass="com.isowire.example.SampleIsoWireClient" -Dexec.args="localhost 9999"
```
```

## Troubleshooting

### Port Already in Use

```bash
# Find process using port 9999
lsof -i :9999

# Kill process
kill -9 <PID>

# Or use different port
./start-server.sh 8888
```

### Field Configuration Not Loading

```bash
# Check YAML file location
ls src/main/resources/iso8583-fields.yaml

# Verify YAML syntax
python3 -c "import yaml; yaml.safe_load(open('src/main/resources/iso8583-fields.yaml'))"

# Check if loaded in target
ls target/classes/iso8583-fields.yaml
```

### Connection Refused

```bash
# Verify server is running
netstat -an | grep 9999

# Check firewall rules
sudo iptables -L | grep 9999

# Test connection manually
telnet localhost 9999
```

## Performance Tips

1. **Virtual Threads**: Java 25 virtual threads handle high concurrency automatically
   ```java
   ISOServer server = new ISOServer(9999); // virtual threads scale automatically
   ```

2. **Channel Reuse**: Reuse channels when possible instead of reconnecting

3. **Async Processing**: Virtual threads enable efficient async I/O

4. **Logging**: Set appropriate log levels in production
   ```xml
   <logger name="com.isowire.iso" level="INFO"/>
   ```

## Security Considerations

1. **PIN Data**: Field 52 contains PIN block data - always encrypt
2. **Track Data**: Fields 35, 36 contain track data - use secure encryption
3. **MAC**: Field 128 should contain Message Authentication Code
4. **Network**: Use SSL/TLS for production connections
5. **Validation**: Always validate input fields and lengths
6. **Compliance**: Follow PCI DSS guidelines for cardholder data

## Use Cases

IsoWire is ideal for:
- **Payment Gateways**: Authorization and settlement processing
- **ATM Systems**: Cash withdrawal and balance inquiries
- **POS Terminals**: Card-present transaction processing
- **Banking Interfaces**: Switch and network connectivity
- **Testing Tools**: ISO8583 message simulation and testing

## Integration

### Spring Boot Integration

```java
@Configuration
public class IsoWireConfig {

   @Bean
   public ISOServer isoServer(ISORequestListener requestListener) {
      ISOServer server = new ISOServer(9999, channelSupplier("ASCII4"));
      server.setRequestListener(requestListener);
      return server;
   }

   @Bean
   public Supplier<ISOServerChannel> channelSupplier(String channelType) {
      return () -> {
         BaseISOChannel channel = switch (channelType) {
            case "BINARY2" -> new Binary2ISOChannel(new ISODefaultPackager(), new TCPTransportChannel());
            default        -> new ASCII4ISOChannel(new ISODefaultPackager(), new TCPTransportChannel());
         };

         channel.setHeader("6000000000");
         return channel;
      };
   }

   @Bean
   public SmartLifecycle isoServerLifecycle(ISOServer server) {
      return new SmartLifecycle() {

         @Override
         public void start() {
            Thread.ofVirtual()
                    .name("isowire-server")
                    .start(() -> {
                       try {
                          server.start();
                       } catch (IOException e) {
                          throw new UncheckedIOException(e);
                       }
                    });
         }

         @Override
         public void stop() {
            server.stop();
         }

         @Override
         public boolean isRunning() {
            return server.isRunning();
         }

         @Override
         public boolean isAutoStartup() {
            return true;
         }
      };
   }
}
```


### Dependency Injection

```java
@Service
public class PaymentService {
    
    private final ASCIIISOChannel channel;
    
    @Autowired
    public PaymentService(ASCIIISOChannel channel) {
        this.channel = channel;
    }
    
    public ISOMessage processPayment(ISOMessage request) throws ISOException {
        return channel.sendAndWait(request, 30000);
    }
}
```

## Next Steps

- Explore `src/main/resources/iso8583-fields.yaml` for all 120+ pre-configured fields
- Review `isowire-examples/src/main/java/com/isowire/example/SampleIsoWireServerLauncher.java` for server setup
- Check `isowire-examples/src/main/java/com/isowire/example/SampleIsoWireClient.java` for client implementation
- See `README.md` for comprehensive documentation
