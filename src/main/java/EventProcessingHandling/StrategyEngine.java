package EventProcessingHandling;

import ConnectionHandling.TwsIB;
import ExecutionHandling.OrderManager;

public class StrategyEngine {

    /* VALUES */
    private final double price;
    private final double SMA_200;
    private final double RSI_2;

    public StrategyEngine(double price){
        this.price = price;

        /* TECHNICAL INDICATORS */
        SMA_200 = TwsIB.SMA_200;
        RSI_2 = TwsIB.RSI_2;

        /* NOT 0.0 DATA VERIFICATION */
        if(price != 0.0 && SMA_200 != 0.0 && RSI_2 != 0.0) {

            long startMs = System.currentTimeMillis();

            /* UPDATE liveData */
            TwsIB.liveData.incrementAnalysis();
            TwsIB.liveData.setCurrentPrice(price);
            TwsIB.liveData.update();

            /* CONDITION : price > SMA(200) && RSI(2) < 10 */
            // Adapt RSI_LIMIT_BUY to accuracy using strategy_data
            double accuracy = TwsIB.strategyData.getAccuracy();
            double RSI_LIMIT_BUY = 10 - (accuracy*5);
            if (price > SMA_200 && RSI_2 < RSI_LIMIT_BUY){

                /* UPDATE liveData */
                TwsIB.liveData.incrementOrder();
                TwsIB.liveData.update();

                /* PLACE BUY ORDER */
                OrderManager orderHandler = new OrderManager();
                orderHandler.placeBuyOrder(price);
                System.out.println("> BUY ORDER HAS BEEN PLACED NOW");
                print();
            }

            /* CONDITION : price < SMA(200) && RSI(2) > 90 */
            // Adapt RSI_LIMIT_SELL to accuracy using strategy_data
            double RSI_LIMIT_SELL = 90 + (accuracy*5);
            if (price < SMA_200 && RSI_2 > RSI_LIMIT_SELL){

                /* UPDATE liveData */
                TwsIB.liveData.incrementOrder();
                TwsIB.liveData.update();

                /* PLACE SELL ORDER */
                OrderManager orderHandler = new OrderManager();
                orderHandler.placeSellOrder(price);
                System.out.println("> SELL ORDER HAS BEEN PLACED NOW");
                print();
            }

            /* UPDATE liveData */
            long stopMS = System.currentTimeMillis();
            TwsIB.liveData.setAnalysisTime(stopMS - startMs);
            TwsIB.liveData.update();
        }
    }

    private void print(){
        /* SHOW TECHNICAL ANALYSIS */
        System.out.println("\n**** START ****");
        System.out.println(" > PRICE : "+price);
        System.out.println(" > SMA200 : "+SMA_200);
        System.out.println(" > RSI2 : "+RSI_2);
        System.out.println("***** END *****\n");
    }
}
