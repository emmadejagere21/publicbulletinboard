import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BulletinBoardInterface extends Remote {
    void add(int index, byte[] value, String tag) throws RemoteException;
    byte[] get(int index, String preImage) throws RemoteException;
    int size() throws RemoteException;
}
