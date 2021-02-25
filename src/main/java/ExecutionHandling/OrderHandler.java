package ExecutionHandling;

import ConnectionHandling.Run;
import com.ib.client.*;
import com.ib.controller.ApiController;

import java.util.ArrayList;
import java.util.List;

public class OrderHandler implements ApiController.ILiveOrderHandler, ApiController.IOrderHandler {

    //This is taken straight from the API Documentation, with some minor modifications
    private static List<Order> BracketOrder(int parentOrderId, Types.Action action, int quantity, double limitPrice, double takeProfitLimitPrice, double stopLossPrice) {

        //This will be our main or "parent" order
        Order parent = new Order();
        parent.orderId(parentOrderId);
        parent.action(action);
        parent.orderType(OrderType.LMT);
        parent.totalQuantity(quantity);
        parent.lmtPrice(limitPrice);
        //The parent and children orders will need this attribute set to false to prevent accidental executions.
        //The LAST CHILD will have it set to true.
        parent.transmit(false);

        Order takeProfit = new Order();
        takeProfit.orderId(parent.orderId() + 1);
        takeProfit.action(action.equals(Types.Action.BUY) ? Types.Action.SELL : Types.Action.BUY);
        takeProfit.orderType(OrderType.LMT);
        takeProfit.totalQuantity(quantity);
        takeProfit.lmtPrice(takeProfitLimitPrice);
        takeProfit.parentId(parentOrderId);
        takeProfit.transmit(false);

        Order stopLoss = new Order();
        stopLoss.orderId(parent.orderId() + 2);
        stopLoss.action(action.equals(Types.Action.BUY) ? Types.Action.SELL : Types.Action.BUY);
        stopLoss.orderType(OrderType.STP);
        //Stop trigger price
        stopLoss.auxPrice(stopLossPrice);
        stopLoss.totalQuantity(quantity);
        stopLoss.parentId(parentOrderId);
        //In this case, the low side order will be the last child being sent. Therefore, it needs to set this attribute to true
        //to activate all its predecessors
        stopLoss.transmit(true);

        List<Order> bracketOrder = new ArrayList<>();
        bracketOrder.add(parent);
        bracketOrder.add(takeProfit);
        bracketOrder.add(stopLoss);
        return bracketOrder;
    }

    //Contract initializer for simplicity
    static Contract initializeContract(){
        Contract nq = new Contract();
        nq.symbol("EUR");
        nq.secType("CASH");
        nq.currency("USD");
        nq.exchange("IDEALPRO");
        return nq;
    }

    //Implementation of the method to create bracket orders
    public void placeBracketOrder(int parentOrderId, Types.Action action, int quantity, double limitPrice, double takeProfitLimitPrice, double stopLossPrice){
        List<Order> bracketOrder = BracketOrder(parentOrderId,action,quantity,limitPrice,takeProfitLimitPrice,stopLossPrice);
        for(Order o : bracketOrder) {
            Run.apiController.placeOrModifyOrder(initializeContract(), o,this);
        }
    }

    @Override
    public void orderState(OrderState orderState) {
    }

    @Override
    public void orderStatus(OrderStatus orderStatus, double v, double v1, double v2, int i, int i1, double v3, int i2, String s, double v4) {
    }

    @Override
    public void handle(int errorCode, String errorMsg) {
    }

    @Override
    public void openOrder(Contract contract, Order order, OrderState orderState) {
    }

    @Override
    public void openOrderEnd() {
    }

    @Override
    public void orderStatus(int i, OrderStatus orderStatus, double v, double v1, double v2, int i1, int i2, double v3, int i3, String s, double v4) {

    }
    @Override
    public void handle(int orderId, int errorCode, String errorMsg) {
    }
}
