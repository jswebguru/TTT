package StorageHandling;

import UserInterface.Screen.Observer;

import java.io.*;
import java.util.ArrayList;

public class AlphaVantageData implements Subject, Serializable {

    private static final long serialVersionUID = -7238850416677010319L;

    /* LIST OF OBSERVERS */
    private transient ArrayList<Observer> observers;

    /* VALUES */
    private String API_KEY = "";

    public AlphaVantageData() {
        observers = new ArrayList<>();
    }

    @Override
    public void registerObserver(Observer o) {
        if (observers == null){
            observers = new ArrayList<>();
        }
        observers.add(o);
    }

    @Override
    public void removeObserver(Observer o) {
        if (observers == null){
            return;
        }
        observers.remove(o);
    }

    @Override
    public void notifyObservers() {
        if (observers == null){
            return;
        }
        for (Observer o : observers){
            o.update();
        }
    }

    public void serialize() {
        /* SERIALIZE alpha.bullitt IN user.home */
        ObjectOutputStream oos = null;
        try {
            String home = System.getProperty("user.home");
            File file = new File(home+"/alpha.bullitt");

            FileOutputStream out = new FileOutputStream(file);
            oos = new ObjectOutputStream(out);
            oos.writeObject(this);
        }
        catch(
                IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* AlphaVantageData METHOD(GETTER AND SETTER) */
    public String getAPI_KEY() {
        return API_KEY;
    }

    public void setAPI_KEY(String API_KEY) {
        this.API_KEY = API_KEY;

        notifyObservers();
        serialize();
    }
}
