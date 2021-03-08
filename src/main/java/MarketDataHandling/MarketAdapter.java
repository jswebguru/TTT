package MarketDataHandling;

import EventProcessingHandling.StrategyEngine;
import ConnectionHandling.TwsIB;

import java.sql.Time;
import java.time.LocalTime;

public class MarketAdapter {

    private long referenceMS;
    private final long delayMS;

    public MarketAdapter() {
        referenceMS = System.currentTimeMillis();

        String timescale = TwsIB.strategyData.getTimescale();
        String delayString = timescale.replace(" sec","");
        delayMS = Long.parseLong(delayString)*1000;

        LocalTime startTime = new Time(System.currentTimeMillis()).toLocalTime();
        TwsIB.liveData.setStartTime(startTime);
        TwsIB.liveData.update();
    }

    public void tickPrice(double price) {

        //TODO : REMOVE
        new StrategyEngine(1.28); //Check for signal

        /* Launch signal according to timescale using strategy_data */
        if (price == -1.0){
            return;
        }

        long currentMS = System.currentTimeMillis();
        if ((currentMS - referenceMS) >= delayMS){
            referenceMS = currentMS;
            System.out.println("> ANALYSIS DONE AT "+new Time(System.currentTimeMillis()).toString());
            new StrategyEngine(price); //Check for signal
        }
    }
}
