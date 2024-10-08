package ConnectionHandling;

import UserInterface.JFrameBTWS;
import com.ib.client.*;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InputAdapterIB implements EWrapper {

    /* COMPONENTS */
    private final EJavaSignal m_signal = new EJavaSignal();
    private final EClientSocket m_client = new EClientSocket( this, m_signal);
    private EReader m_reader;

    public EClientSocket onConnect() {
        if(m_client.isConnected())
            return m_client;

        m_client.eConnect( "127.0.0.1",7497, 2);
        if (m_client.isConnected()) {
            String msg = "> Connected to Tws server version " + m_client.serverVersion() + " at " + m_client.getTwsConnectionTime();
            System.out.println(msg);
        }

        m_reader = new EReader(m_client, m_signal);

        m_reader.start();

        new Thread(this::processMessages).start();

        return m_client;
    }

    private void processMessages() {
        while (m_client.isConnected()) {
            m_signal.waitForSignal();
            try {
                m_reader.processMsgs();
            } catch (Exception e) {
                error(e);
            }
        }
    }

    @Override
    public void tickPrice(int tickerId, int field, double price, TickAttrib attribs) {
        if (field == 2){ // askPrice
            TwsIB.marketAdapter.tickPrice(price);
            //String msg = EWrapperMsgGenerator.tickPrice( tickerId, field, price, attribs);
            //System.out.println(msg);
        }
    }

    @Override
    public void orderStatus( int orderId, String status, double filled, double remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
        try {
            /* UPDATE orderStatus WITHIN historyData */
            if (parentId == 0){ //parent
                TwsIB.historyData.getTransaction(orderId).setStatus(status);
                TwsIB.historyData.getTransaction(orderId).setAvgFillPrice(avgFillPrice);
                TwsIB.historyData.update();
            }else {
                if (TwsIB.historyData.getTransaction(parentId).getOrderId_tp() == orderId){ //tp
                    TwsIB.historyData.getTransaction(parentId).setStatus_tp(status);
                    TwsIB.historyData.getTransaction(parentId).setAvgFillPrice_tp(avgFillPrice);
                    TwsIB.historyData.update();
                } else if (TwsIB.historyData.getTransaction(parentId).getOrderId_sl() == orderId){ //sl
                    TwsIB.historyData.getTransaction(parentId).setStatus_sl(status);
                    TwsIB.historyData.getTransaction(parentId).setAvgFillPrice_sl(avgFillPrice);
                    TwsIB.historyData.update();
                }
            }
        }catch (NullPointerException e){
            // OTHERS ORDERS NOT PASSED BY THE SYSTEM
        }
        //String msg = EWrapperMsgGenerator.orderStatus( orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld, mktCapPrice);
        //System.out.println(msg);
    }

    @Override
    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
        try {
            /* UPDATE openOrder WITHIN historyData */
            if (order.parentId() == 0) { //parent
                TwsIB.historyData.getTransaction(order.orderId()).setStatus(orderState.getStatus());
                TwsIB.historyData.update();
            } else if (order.parentId() != 0) {
                if (order.orderType() == OrderType.LMT) { //tp
                    TwsIB.historyData.getTransaction(order.parentId()).setStatus_tp(orderState.getStatus());
                    TwsIB.historyData.update();
                } else if (order.orderType() == OrderType.STP) { //sl
                    TwsIB.historyData.getTransaction(order.parentId()).setStatus_sl(orderState.getStatus());
                    TwsIB.historyData.update();
                }
            }
        }catch (NullPointerException e){
            // OTHER TRANSACTIONS NOT PASSED BY THE SYSTEM
        }
        //String msg = EWrapperMsgGenerator.openOrder( orderId, contract, order, orderState);
        //System.out.println(msg);
    }

    @Override
    public void openOrderEnd() {
        //String msg = EWrapperMsgGenerator.openOrderEnd();
        //System.out.println(msg);
    }

    @Override
    public void nextValidId(int i) {
        TwsIB.setNextValidID(i);
        //System.out.println("nextValidID : "+i);
    }

    @Override
    public void managedAccounts(String accountsList) {
        TwsIB.accountData.setAccountId(accountsList);
        TwsIB.accountData.update();
        //String msg = EWrapperMsgGenerator.managedAccounts(accountsList);
        //System.out.println(msg);
    }

    @Override
    public void position(String account, Contract contract, double pos, double avgCost) {
        if (!contract.localSymbol().equals(TwsIB.strategyData.getAsset())){
            return;
        }

        TwsIB.positionData.setAccount(account);
        TwsIB.positionData.setContract(contract);
        TwsIB.positionData.setPos(pos);
        TwsIB.positionData.setAvgCost(avgCost);
        TwsIB.positionData.update();

        if (!OutputAdapterIB.statusReqPnLSingle){
            TwsIB.outputAdapterIB.onReqPnLSingle();
        }
        //String msg = EWrapperMsgGenerator.position(account, contract, pos, avgCost);
        //System.out.println(msg);
    }

    @Override
    public void positionEnd() {
        //String msg = EWrapperMsgGenerator.positionEnd();
        //System.out.println(msg);
    }

    @Override
    public void accountSummary(int reqId, String account, String tag, String value, String currency) {
        switch (tag){
            case "NetLiquidation":
                TwsIB.accountData.setNetLiquidationValue(Double.parseDouble(value));
                break;
            case "BuyingPower":
                TwsIB.accountData.setBuyingPower(Double.parseDouble(value));
                break;
            case "AvailableFunds":
                TwsIB.accountData.setAvailableFunds(Double.parseDouble(value));
                TwsIB.accountData.setCurrency(currency);
                break;
            case "Cushion":
                BigDecimal cushion = new BigDecimal(value);
                cushion = cushion.setScale(2, RoundingMode.HALF_UP);
                TwsIB.accountData.setCushion(cushion.doubleValue()*100);
                break;
            case "FullInitMarginReq":
                TwsIB.accountData.setMarginReq(Double.parseDouble(value));
                break;
        }

        TwsIB.accountData.update();
        //String msg = EWrapperMsgGenerator.accountSummary(reqId, account, tag, value, currency);
        //System.out.println(msg);
    }

    @Override
    public void accountSummaryEnd(int reqId) {
        //String msg = EWrapperMsgGenerator.accountSummaryEnd(reqId);
        //System.out.println(msg);
    }

    @Override
    public void error(Exception e) {
        e.printStackTrace();
    }

    @Override
    public void error(String s) {
        System.err.println(s);
    }

    @Override
    public void error(int i, int i1, String s) {
        System.err.println(i+" / "+i1+" / "+s);

        /* KILL THE SYSTEM IF NO CONNECTION AVAILABLE */
        if (s.equals("Not connected") || s.equals("Bad Message Length null")){
            JOptionPane.showMessageDialog(JFrameBTWS.getInstance().getRootPane(), "The system failed to connect to the trading workstation. Please ensure that the trading workstation is active and operational before restarting the algorithm.");
            System.exit(-1);
        }
    }

    @Override
    public void connectionClosed() {
        String msg = EWrapperMsgGenerator.connectionClosed();
        System.out.println(msg);
    }

    @Override
    public void connectAck() {
        if (m_client.isAsyncEConnect())
            m_client.startAPI();
    }

    @Override
    public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {
        BigDecimal dPNL = new BigDecimal(Double.toString(dailyPnL));
        dPNL = dPNL.setScale(2, RoundingMode.HALF_UP);
        TwsIB.accountData.setDailyPNL(dPNL.doubleValue());
        TwsIB.liveData.setDailyPNL(dPNL.doubleValue());

        BigDecimal uPNL = new BigDecimal(Double.toString(unrealizedPnL));
        uPNL = uPNL.setScale(2, RoundingMode.HALF_UP);
        TwsIB.accountData.setUnrealizedPNL(uPNL.doubleValue());
        TwsIB.liveData.setUnrealizedPNL(uPNL.doubleValue());

        BigDecimal rPNL = new BigDecimal(Double.toString(realizedPnL));
        rPNL = rPNL.setScale(2, RoundingMode.HALF_UP);
        TwsIB.accountData.setRealizedPNL(rPNL.doubleValue());
        TwsIB.liveData.setRealizedPNL(rPNL.doubleValue());
        TwsIB.accountData.update();
        TwsIB.liveData.update();
        //String msg = EWrapperMsgGenerator.pnl(reqId, dailyPnL, unrealizedPnL, realizedPnL);
        //System.out.println(msg);
    }

    @Override
    public void pnlSingle(int reqId, int pos, double dailyPnL, double unrealizedPnL, double realizedPnL, double value) {
        if (dailyPnL == Double.MAX_VALUE){
            TwsIB.positionData.setDailyPnL(0.0);
        }else {
            BigDecimal dPNL = new BigDecimal(Double.toString(dailyPnL));
            dPNL = dPNL.setScale(2, RoundingMode.HALF_UP);
            TwsIB.positionData.setDailyPnL(dPNL.doubleValue());
        }

        if (unrealizedPnL == Double.MAX_VALUE){
            TwsIB.positionData.setUnrealizedPnL(0.0);
        }else {
            BigDecimal uPNL = new BigDecimal(Double.toString(unrealizedPnL));
            uPNL = uPNL.setScale(2, RoundingMode.HALF_UP);
            TwsIB.positionData.setUnrealizedPnL(uPNL.doubleValue());
        }

        if (realizedPnL == Double.MAX_VALUE){
            TwsIB.positionData.setRealizedPnL(0.0);
        }else {
            BigDecimal rPNL = new BigDecimal(Double.toString(realizedPnL));
            rPNL = rPNL.setScale(2, RoundingMode.HALF_UP);
            TwsIB.positionData.setRealizedPnL(rPNL.doubleValue());
        }

        if (value == Double.MAX_VALUE){
            TwsIB.positionData.setValue(0.0);
        }else {
            BigDecimal val = new BigDecimal(Double.toString(value));
            val = val.setScale(2, RoundingMode.HALF_UP);
            TwsIB.positionData.setValue(val.doubleValue());
        }
        TwsIB.positionData.update();
        //String msg = EWrapperMsgGenerator.pnlSingle(reqId, pos, dailyPnL, unrealizedPnL, realizedPnL, value);
        //System.out.println(msg);
    }

    @Override
    public void tickSize(int i, int i1, int i2) {}

    @Override
    public void tickOptionComputation(int i, int i1, double v, double v1, double v2, double v3, double v4, double v5, double v6, double v7) {}

    @Override
    public void tickGeneric(int i, int i1, double v) {}

    @Override
    public void tickString(int i, int i1, String s) {}

    @Override
    public void tickEFP(int i, int i1, double v, String s, double v1, int i2, String s1, double v2, double v3) {}

    @Override
    public void updateAccountValue(String s, String s1, String s2, String s3) {}

    @Override
    public void updatePortfolio(Contract contract, double v, double v1, double v2, double v3, double v4, double v5, String s) {}

    @Override
    public void updateAccountTime(String s) {}

    @Override
    public void accountDownloadEnd(String s) {}

    @Override
    public void contractDetails(int i, ContractDetails contractDetails) {}

    @Override
    public void bondContractDetails(int i, ContractDetails contractDetails) {}

    @Override
    public void contractDetailsEnd(int i) {}

    @Override
    public void execDetails(int i, Contract contract, Execution execution) {}

    @Override
    public void execDetailsEnd(int i) {}

    @Override
    public void updateMktDepth(int i, int i1, int i2, int i3, double v, int i4) {}

    @Override
    public void updateMktDepthL2(int i, int i1, String s, int i2, int i3, double v, int i4, boolean b) {}

    @Override
    public void updateNewsBulletin(int i, int i1, String s, String s1) {}

    @Override
    public void receiveFA(int i, String s) {}

    @Override
    public void historicalData(int i, Bar bar) {}

    @Override
    public void scannerParameters(String s) {}

    @Override
    public void scannerData(int i, int i1, ContractDetails contractDetails, String s, String s1, String s2, String s3) {}

    @Override
    public void scannerDataEnd(int i) {}

    @Override
    public void realtimeBar(int i, long l, double v, double v1, double v2, double v3, long l1, double v4, int i1) {}

    @Override
    public void currentTime(long l) {}

    @Override
    public void fundamentalData(int i, String s) {}

    @Override
    public void deltaNeutralValidation(int i, DeltaNeutralContract deltaNeutralContract) {}

    @Override
    public void tickSnapshotEnd(int i) {}

    @Override
    public void marketDataType(int i, int i1) {}

    @Override
    public void commissionReport(CommissionReport commissionReport) {}

    @Override
    public void verifyMessageAPI(String s) {}

    @Override
    public void verifyCompleted(boolean b, String s) {}

    @Override
    public void verifyAndAuthMessageAPI(String s, String s1) {}

    @Override
    public void verifyAndAuthCompleted(boolean b, String s) {}

    @Override
    public void displayGroupList(int i, String s) {}

    @Override
    public void displayGroupUpdated(int i, String s) {}

    @Override
    public void positionMulti(int i, String s, String s1, Contract contract, double v, double v1) {}

    @Override
    public void positionMultiEnd(int i) {}

    @Override
    public void accountUpdateMulti(int reqId, String account, String modelCode, String key, String value, String currency) {}

    @Override
    public void accountUpdateMultiEnd(int reqId) {}

    @Override
    public void securityDefinitionOptionalParameter(int i, String s, int i1, String s1, String s2, Set<String> set, Set<Double> set1) {}

    @Override
    public void securityDefinitionOptionalParameterEnd(int i) {}

    @Override
    public void softDollarTiers(int i, SoftDollarTier[] softDollarTiers) {}

    @Override
    public void familyCodes(FamilyCode[] familyCodes) {}

    @Override
    public void symbolSamples(int i, ContractDescription[] contractDescriptions) {}

    @Override
    public void historicalDataEnd(int i, String s, String s1) {}

    @Override
    public void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions) {}

    @Override
    public void tickNews(int i, long l, String s, String s1, String s2, String s3) {}

    @Override
    public void smartComponents(int i, Map<Integer, Map.Entry<String, Character>> map) {}

    @Override
    public void tickReqParams(int i, double v, String s, int i1) {}

    @Override
    public void newsProviders(NewsProvider[] newsProviders) {}

    @Override
    public void newsArticle(int i, int i1, String s) {}

    @Override
    public void historicalNews(int i, String s, String s1, String s2, String s3) {}

    @Override
    public void historicalNewsEnd(int i, boolean b) {}

    @Override
    public void headTimestamp(int i, String s) {}

    @Override
    public void histogramData(int i, List<HistogramEntry> list) {}

    @Override
    public void historicalDataUpdate(int i, Bar bar) {}

    @Override
    public void rerouteMktDataReq(int i, int i1, String s) {}

    @Override
    public void rerouteMktDepthReq(int i, int i1, String s) {}

    @Override
    public void marketRule(int i, PriceIncrement[] priceIncrements) {}

    @Override
    public void historicalTicks(int i, List<HistoricalTick> list, boolean b) {}

    @Override
    public void historicalTicksBidAsk(int i, List<HistoricalTickBidAsk> list, boolean b) {}

    @Override
    public void historicalTicksLast(int i, List<HistoricalTickLast> list, boolean b) {}

    @Override
    public void tickByTickAllLast(int i, int i1, long l, double v, int i2, TickAttribLast tickAttribLast, String s, String s1) {}

    @Override
    public void tickByTickBidAsk(int i, long l, double v, double v1, int i1, int i2, TickAttribBidAsk tickAttribBidAsk) {}

    @Override
    public void tickByTickMidPoint(int i, long l, double v) {}

    @Override
    public void orderBound(long l, int i, int i1) {}

    @Override
    public void completedOrder(Contract contract, Order order, OrderState orderState) {}

    @Override
    public void completedOrdersEnd() {}
}
