package StorageHandling;

import UserInterface.Screen.Observer;
import com.ib.client.Types;

import java.util.*;

public class HistoryData implements Subject {

    /* LIST OF OBSERVERS */
    private ArrayList<Observer> observers;

    /* VALUES */
    private final Map<Integer, TransactionDTO> transactions;

    public HistoryData() {
        transactions = new LinkedHashMap<>();
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

    /* HistoryData METHOD(GETTER AND SETTER) */
    public List<TransactionDTO> getTransactions (){
        List<TransactionDTO> transactionDTOS = new ArrayList<>();

        for (Map.Entry<Integer,TransactionDTO> entry : transactions.entrySet()){
            transactionDTOS.add(entry.getValue());
        }
        Collections.reverse(transactionDTOS);

        return transactionDTOS;
    }

    public TransactionDTO getTransaction (int id){
        return transactions.get(id);
    }

    public int getNumberBuy() {
        int i = 0;
        for (Map.Entry<Integer,TransactionDTO> entry : transactions.entrySet()){
            if (entry.getValue().getAction() == Types.Action.BUY){
                i++;
            }
        }
        return i;
    }

    public int getNumberSell() {
        int i = 0;
        for (Map.Entry<Integer,TransactionDTO> entry : transactions.entrySet()){
            if (entry.getValue().getAction() == Types.Action.SELL){
                i++;
            }
        }
        return i;
    }

    public double getExposition() {
        double sum = 0;
        for (Map.Entry<Integer,TransactionDTO> entry : transactions.entrySet()){
            sum += Math.abs(entry.getValue().getQuantity()*entry.getValue().getAvgFillPrice());
        }
        return sum;
    }

    public void addTransactions (TransactionDTO transactionDTO){

        Integer id = transactionDTO.getOrderId();
        transactions.put(id, transactionDTO);

        notifyObservers();
    }

    public void update (){
        notifyObservers();
    }

    public void reset (){
        transactions.clear();
        notifyObservers();
    }
}
