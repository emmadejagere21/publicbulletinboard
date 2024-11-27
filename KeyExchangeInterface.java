import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;

public interface KeyExchangeInterface extends Remote {
    PublicKey exchangePublicKey(PublicKey clientPublicKey) throws RemoteException;
}