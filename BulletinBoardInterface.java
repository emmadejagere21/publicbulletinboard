import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BulletinBoardInterface extends Remote {
    void add(int index, byte[] value, String tag) throws RemoteException;
    byte[] get(int index, String preImage) throws RemoteException;
    int size() throws RemoteException;
    void updateClientStatus(String clientTag, boolean status) throws RemoteException;
    boolean isClientOnline(String clientTag) throws RemoteException;
    void saveOfflineMessage(String tag, byte[] message) throws RemoteException;
    List<byte[]> getOfflineMessages(String tag) throws RemoteException;

}
