package br.com.casacesar.luz;
import android.content.Context;import android.security.keystore.*;import android.util.Base64;
import java.io.File;import java.nio.charset.StandardCharsets;import java.nio.file.Files;import java.security.*;import java.security.interfaces.RSAPublicKey;import javax.crypto.Cipher;import org.json.JSONObject;

/** Configuration remains in this installation's encrypted private preferences. */
final class TriggerCmd {
 private static final String PAIR_ALIAS="CasaLampada.TriggerCmd.Pair.v1";
 private final Context context;private final SessionStore store;private final TriggerCmdProtocol protocol=new TriggerCmdProtocol();private long lastShutdown;
 TriggerCmd(Context c){context=c;store=new SessionStore(c,"triggercmd","CasaLampada.TriggerCmd.v1");}
 JSONObject status()throws Exception{JSONObject c=store.load();return new JSONObject().put("configured",!c.optString("token").isEmpty()).put("computer",c.optString("computer")).put("trigger",c.optString("trigger")).put("verified",c.optBoolean("verified"));}
 JSONObject save(JSONObject input)throws Exception{JSONObject old=store.load(),config=new JSONObject().put("computer",input.optString("computer")).put("trigger",input.optString("trigger")).put("token",input.optString("token").trim().isEmpty()?old.optString("token"):input.getString("token"));TriggerCmdProtocol.validateConfig(config);store.saveObject(config);return status();}
 JSONObject check()throws Exception{JSONObject config=store.load(),result=protocol.check(config);config.put("verified",true);store.saveObject(config);return result;}
 JSONObject shutdown()throws Exception{if(System.currentTimeMillis()-lastShutdown<30000)throw new Exception("Aguarde 30 segundos antes de enviar outro desligamento.");JSONObject result=protocol.shutdown(store.load());lastShutdown=System.currentTimeMillis();return result;}
 JSONObject clear()throws Exception{store.clear();return status();}
 JSONObject preparePair()throws Exception{
  KeyPairGenerator generator=KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA,"AndroidKeyStore");generator.initialize(new KeyGenParameterSpec.Builder(PAIR_ALIAS,KeyProperties.PURPOSE_DECRYPT).setKeySize(3072).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP).setDigests(KeyProperties.DIGEST_SHA1).build());KeyPair pair=generator.generateKeyPair();RSAPublicKey key=(RSAPublicKey)pair.getPublic();
  JSONObject publicKey=new JSONObject().put("modulus",Base64.encodeToString(key.getModulus().toByteArray(),Base64.NO_WRAP)).put("exponent",Base64.encodeToString(key.getPublicExponent().toByteArray(),Base64.NO_WRAP));
  File directory=context.getExternalFilesDir(null);if(directory==null)throw new Exception("Armazenamento de vínculo indisponível.");Files.write(new File(directory,"triggercmd-public-key.json").toPath(),publicKey.toString().getBytes(StandardCharsets.UTF_8));Files.deleteIfExists(new File(directory,"triggercmd-pair.enc").toPath());return new JSONObject().put("prepared",true).put("message","Chave pública preparada. Conecte o USB e execute Vincular TRIGGERcmd no PC; depois toque em Importar vínculo.");
 }
 JSONObject importPair()throws Exception{
  File directory=context.getExternalFilesDir(null);if(directory==null)throw new Exception("Armazenamento de vínculo indisponível.");File file=new File(directory,"triggercmd-pair.enc");if(!file.exists())throw new Exception("Vínculo não encontrado. Execute primeiro o auxiliar no PC.");if(file.length()!=384)throw new Exception("Arquivo de vínculo inválido.");
  KeyStore keys=KeyStore.getInstance("AndroidKeyStore");keys.load(null);PrivateKey key=(PrivateKey)keys.getKey(PAIR_ALIAS,null);if(key==null)throw new Exception("Prepare um novo vínculo neste celular.");Cipher cipher=Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");cipher.init(Cipher.DECRYPT_MODE,key);byte[] bytes=cipher.doFinal(Files.readAllBytes(file.toPath()));
  try{JSONObject config=new JSONObject(new String(bytes,StandardCharsets.UTF_8));TriggerCmdProtocol.validateConfig(config);store.saveObject(config);Files.deleteIfExists(file.toPath());Files.deleteIfExists(new File(directory,"triggercmd-public-key.json").toPath());keys.deleteEntry(PAIR_ALIAS);return status();}finally{java.util.Arrays.fill(bytes,(byte)0);}
 }
}
