import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;

public class KeyExchangeServer extends UnicastRemoteObject implements KeyExchangeInterface {
    private PublicKey serverPublicKey;

    protected KeyExchangeServer() throws RemoteException {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();
            serverPublicKey = keyPair.getPublic();
        } catch (Exception e) {
            throw new RemoteException("Error initializing Diffie-Hellman key pair", e);
        }
    }

    @Override
    public PublicKey exchangePublicKey(PublicKey clientPublicKey) throws RemoteException {
        System.out.println("Received client public key: " + clientPublicKey);
        return serverPublicKey;
    }
}
