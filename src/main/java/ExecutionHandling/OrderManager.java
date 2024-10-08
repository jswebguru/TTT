package ExecutionHandling;

import StorageHandling.TransactionDTO;
import ConnectionHandling.TwsIB;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types;

import java.util.ArrayList;
import java.util.List;

public class OrderManager {

    /* CREATE BRACKET ORDER (PARENT, TP, SL) */
    public static List<Order> BracketOrder(Types.Action action, int quantity, double limitPrice, double takeProfitLimitPrice, double stopLossPrice) {

        /* GET ID FROM TWS IB */
        int parentOrderId = TwsIB.getNextValidIDOrder();

        /* PARENT ORDER */
        Order parent = new Order();
        parent.orderId(parentOrderId);
        parent.action(action);
        parent.totalQuantity(quantity);
        /* Adapt order type using strategy_data */
        switch (TwsIB.strategyData.getOrder()){
            case "Market":
                parent.orderType(OrderType.MKT);
                break;
            case "Limit":
                parent.orderType(OrderType.LMT);
                parent.lmtPrice(limitPrice);
                break;
        }
        parent.transmit(false);

        /* TAKE-PROFIT(TP) ORDER */
        Order takeProfit = new Order();
        takeProfit.parentId(parentOrderId+1);
        takeProfit.orderId(parentOrderId+2);
        takeProfit.action(action.equals(Types.Action.BUY) ? Types.Action.SELL : Types.Action.BUY);
        takeProfit.totalQuantity(quantity);
        takeProfit.orderType(OrderType.LMT);
        takeProfit.lmtPrice(takeProfitLimitPrice);
        takeProfit.transmit(false);

        /* STOP-LOSS(SL) ORDER */
        Order stopLoss = new Order();
        stopLoss.parentId(parentOrderId+1);
        stopLoss.orderId(parentOrderId+3);
        stopLoss.action(action.equals(Types.Action.SELL) ? Types.Action.BUY : Types.Action.SELL);
        stopLoss.totalQuantity(quantity);
        stopLoss.orderType(OrderType.STP);
        stopLoss.auxPrice(stopLossPrice);
        stopLoss.transmit(true);

        /* SET OF ORDERS */
        List<Order> orders = new ArrayList<>();
        orders.add(parent);
        orders.add(takeProfit);
        orders.add(stopLoss);
        return orders;
    }

    /* CREATE BUY ORDER */
    public void placeBuyOrder(double price) {
        /* Adapt quantity to multiplier using strategy_data */
        int multiplier = TwsIB.strategyData.getMultiplier();
        int quantity = 20000 * multiplier;

        /* Adapt order to type using strategy_data */
        String order = TwsIB.strategyData.getOrder();
        double limitPrice = 0;
        switch (order){
            case "Market":
                limitPrice = -1;
                break;
            case "Limit":
                limitPrice = price;
                break;
        }

        /* Adapt takeProfitLimitPrice to strategy_data */
        double takeProfit = TwsIB.strategyData.getTake_profit();
        double takeProfitLimitPrice = price * (1.0+(takeProfit/100.0));

        /* Adapt stopLossPrice to strategy_data */
        double stopLoss = TwsIB.strategyData.getStop_loss();
        double stopLossPrice = price * (1.0-(stopLoss/100.0));

        /* Adapt stop loss to SMA_5 => stopLossPrice > SMA(5) */
        double SMA_5 = TwsIB.SMA_5;
        if (stopLossPrice > SMA_5){
            stopLossPrice = SMA_5;
        }

        /* Show buy order */
        System.out.println("\n**** BUY ORDER ****"+
                "\n > Quantity : "+quantity+" <"+
                "\n > Limit Price : "+limitPrice+" <"+
                "\n > TakeProfit Limit Price : "+takeProfitLimitPrice+" <"+
                "\n > StopLoss Limit Price : "+stopLossPrice+" <"+
                "\n******* END ******* \n");

        /* Get bracket order */
        List<Order> orders = BracketOrder(Types.Action.BUY, quantity, round(limitPrice), round(takeProfitLimitPrice), round(stopLossPrice));

        /* Transmit bracket order */
        placeBracketOrder(orders);
    }

    /* CREATE SELL ORDER */
    public void placeSellOrder(double price){
        /* Adapt quantity to multiplier using strategy_data */
        int multiplier = TwsIB.strategyData.getMultiplier();
        int quantity = 20000 * multiplier;

        /* Adapt order to type using strategy_data */
        String order = TwsIB.strategyData.getOrder();
        double limitPrice = 0;
        switch (order){
            case "Market":
                limitPrice = -1;
                break;
            case "Limit":
                limitPrice = price;
                break;
        }

        /* Adapt takeProfitLimitPrice to strategy_data */
        double takeProfit = TwsIB.strategyData.getTake_profit();
        double takeProfitLimitPrice = price * (1.0-(takeProfit/100.0));

        /* Adapt stopLossPrice to strategy_data */
        double stopLoss = TwsIB.strategyData.getStop_loss();
        double stopLossPrice = price * (1.0+(stopLoss/100.0));

        /* Adapt stop loss to SMA_5 => stopLossPrice < SMA(5) */
        double SMA_5 = TwsIB.SMA_5;
        if (stopLossPrice < SMA_5){
            stopLossPrice = SMA_5;
        }

        /* Show sell order */
        System.out.println("\n**** SELL ORDER ****"+
                "\n > Quantity : "+quantity+" <"+
                "\n > Limit Price : "+limitPrice+" <"+
                "\n > TakeProfit Limit Price : "+takeProfitLimitPrice+" <"+
                "\n > StopLoss Limit Price : "+stopLossPrice+" <"+
                "\n******* END ******* \n");

        /* Get bracket order */
        List<Order> orders = BracketOrder(Types.Action.SELL, quantity, round(limitPrice), round(takeProfitLimitPrice), round(stopLossPrice));

        /* Transmit bracket order */
        placeBracketOrder(orders);
    }

    /* PLACE ORDER */
    private void placeBracketOrder (List<Order> bracketOrder) {
        /* VERIFY BRACKET ORDER */
        if (RiskManager.verifyOrder(bracketOrder)) {
            /* TRANSMIT ORDER TO MARKET */
            for (Order o : bracketOrder) {
                TwsIB.outputAdapterIB.onPlaceOrder(o);
            }
            /* ADD TRANSACTIONS TO HISTORY */
            TwsIB.historyData.addTransactions(new TransactionDTO(bracketOrder));
            System.out.println("> The risk management system agreed a transaction.");
        } else {
            System.out.println("> The risk management system blocked a transaction.");
        }
    }

    public double round (double d){
        /* ROUND TO 0.05 FOR MARKET ADAPTATION */
        return Math.round(d * 20.0) / 20.0;
    }
}
