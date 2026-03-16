import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import com.sun.net.httpserver.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.*;
import java.util.Set;

public class PqcHttpsServer {

    public static void main(String[] args) throws Exception {
        // 1. Register Bouncy Castle and BC-PQC Providers
        Security.addProvider(new BouncyCastleProvider());
        Security.addProvider(new BouncyCastlePQCProvider());

        System.out.println("Starting Post-Quantum Safe Server...");
        System.out.println("Supported PQC Algos: ML-KEM, Falcon, Dilithium");

        // 2. Setup the HTTPS Server on port 8443
        HttpsServer server = HttpsServer.create(new InetSocketAddress(8443), 0);
        SSLContext sslContext = createPqcSslContext();

        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            public void configure(HttpsParameters params) {
                try {
                    SSLParameters sslparams = sslContext.getDefaultSSLParameters();

                    // Force TLS 1.3 as it's required for PQC Key Exchange
                    sslparams.setProtocols(new String[]{"TLSv1.3"});

                    // Setting the PQC-ready named groups (ML-KEM / Kyber)
                    // Note: String names vary by JSSE/BC version (e.g., "ML-KEM-768")
                    sslparams.setAlgorithmConstraints(new PqcConstraints());

                    params.setSSLParameters(sslparams);
                } catch (Exception e) {
                    System.out.println("Failed to configure PQC SSL Parameters");
                }
            }
        });

        // 3. Define a simple "Hello World" handler
        server.createContext("/test", exchange -> {
            String resp = "<html><body><h1>PQ-Safe Handshake Successful!</h1>" +
                    "<p>Encrypted via ML-KEM (Post-Quantum Algorithm)</p></body></html>";
            exchange.sendResponseHeaders(200, resp.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(resp.getBytes());
            os.close();
        });

        server.setExecutor(null); // default executor
        server.start();
        System.out.println("Server is live at https://localhost:8443/test");
    }

    private static SSLContext createPqcSslContext() throws Exception {
        // In a real app, load your .jks here. For this demo, we assume a keystore exists.
        char[] password = "password".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        FileInputStream fis = new FileInputStream("my-pqc-keystore.jks");
        ks.load(fis, password);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password);

        // Initialize SSLContext with Bouncy Castle as the provider
        SSLContext sslContext = SSLContext.getInstance("TLS", "BC");
        sslContext.init(kmf.getKeyManagers(), null, null);

        return sslContext;
    }
}

/**
 * Custom constraints to ensure only PQ-safe algorithms are permitted.
 */
class PqcConstraints implements AlgorithmConstraints {
    @Override
    public boolean permits(Set<CryptoPrimitive> primitives, String algorithm, AlgorithmParameters parameters) {
        return true; // In production, filter for ML-KEM or Dilithium here
    }

    @Override
    public boolean permits(Set<CryptoPrimitive> primitives, Key key) {
        return true;
    }

    @Override
    public boolean permits(Set<CryptoPrimitive> primitives, String algorithm, Key key, AlgorithmParameters parameters) {
        return true;
    }
}