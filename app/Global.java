import com.typesafe.config.*;
import java.io.*;
import java.util.*;
import org.apache.commons.codec.binary.Base64;
import play.*;

// https://www.playframework.com/documentation/2.3.x/JavaGlobal
public class Global extends GlobalSettings {

  // https://www.playframework.com/documentation/2.3.x/api/java/play/GlobalSettings.html#onLoadConfig(play.Configuration,%20java.io.File,%20java.lang.ClassLoader,%20play.Mode)
  public Configuration onLoadConfig(Configuration config, File path, ClassLoader classloader) {
    // The original configuration
    Config originalTypesafeConfig = config.underlying();

    // The list of keys to iterate over and tranform their values
    String[] keysToTransform = new String[] {
      "akka.remote.netty.security.ssl.key-store-password",
      "akka.remote.netty.security.ssl.key-password",
      "akka.remote.netty.security.ssl.trust-store-password"
    };

    // Store the new values for the keys in this map
    Map<String, String> overridingConfigValues = new HashMap<String, String>();

    // Iterate over all the keys, transforming them one by one and adding
    // them to the map
    for (int i = 0; i < keysToTransform.length; i++) {
      String key = keysToTransform[i];
      try {
        String originalValue = originalTypesafeConfig.getString(key);
        Logger.info("Transforming config at "+key);
        Logger.info(originalValue);
        String transformedValue = transformConfigValue(originalValue);
        Logger.info(transformedValue);
        overridingConfigValues.put(key, transformedValue);
      } catch (ConfigException.Missing e) {
        Logger.info("No config at "+key);
      }
    }
    // Convert the map of transformed values into a Typesafe Config object
    Config overridingTypesafeConfig = ConfigFactory.parseMap(overridingConfigValues);

    // Combine the transformed values and the original config into a new
    // Typesafe Config object. The transformed values will override the
    // original values.
    Config newTypesafeConfig = overridingTypesafeConfig.withFallback(originalTypesafeConfig);

    // Wrap the Typesafe Config object in a Play Configuration object
    Configuration newPlayConfig = new Configuration(newTypesafeConfig);

    // Return the value for Play to use
    return newPlayConfig;
  }

  /**
   * Transforms a configuration value. This example implementation implements
   * WebSphere's "xor encryption", where a string like "{xor}LDo8LTor" will be
   * decrypted to "secret".
   *
   * @see http://strelitzia.net/wasXORdecoder/wasXORdecoder.html
   */
  static String transformConfigValue(String originalValue) {
    final String ENCRYPTION_PREFIX = "{xor}";
    final byte KEY = (byte) '_';
    if (originalValue.startsWith(ENCRYPTION_PREFIX)) {
      try {
        String base64Encoded = originalValue.substring(ENCRYPTION_PREFIX.length());
        byte[] xorEncoded = Base64.decodeBase64(base64Encoded);
        byte[] decodedBytes = new byte[xorEncoded.length];
        for (int i = 0; i < xorEncoded.length; i++) {
          decodedBytes[i] = (byte) (xorEncoded[i] ^ KEY);
        }
        String decoded = new String(decodedBytes, "UTF-8");
        return decoded;
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException("Error decoding config value", e);
      }
    } else {
      return originalValue;
    }
  }

}