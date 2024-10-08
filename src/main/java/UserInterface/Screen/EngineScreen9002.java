package UserInterface.Screen;

import StorageHandling.AlphaVantageData;
import StorageHandling.StrategyData;
import UserInterface.Component.ImageLabel;
import UserInterface.Component.Panel.ButtonPanel;
import UserInterface.Component.Panel.InfoPanel;
import UserInterface.Component.Enum.EnumType;
import UserInterface.JFrameBTWS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class EngineScreen9002 extends AbstractScreen implements Observer {

    /* COMPONENTS */
    private final InfoPanel[] infoPanels = new InfoPanel[9];
    private final ButtonPanel[] buttonPanels = new ButtonPanel[2];

    /* DATA SUBJECT */
    private final StrategyData strategyData;
    private final AlphaVantageData alphaVantageData;

    /* TOOLS TO START TWS */
    private final MouseListener mouseListener = new startAndStop();
    private String state = "stop";
    private boolean apiKeyAvailable = false;

    /* DATA TO DISPLAY */
    private final String [] IP_texts = new String[]{"Asset", "Strategy", "Accuracy", "Timescale", "Exposure", "Order", "Take profit", "Stop loss", "APIs key"};
    private final String [] IP_values = new String[]{"JPY/USD", "RSI(2)", "0.7", "60 sec", "90%", "Limit", "7.5%", "5%", "Inactive"};
    private final EnumType[] IP_types = new EnumType[]{EnumType.NO_ICON, EnumType.NO_ICON, EnumType.NEUTRAL, EnumType.NEUTRAL, EnumType.NEUTRAL, EnumType.NEUTRAL, EnumType.POSITIVE, EnumType.NEGATIVE, EnumType.NO_ICON };

    /* STATIC DATA TO DISPLAY*/
    private final String [] BT_Header = new String[]{"1. STRATEGY", "2. MONITOR"};
    private final String [] BT_Description = new String[]{"Modify or view the strategy", "View live data"};
    private final String [] BT_Target = new String[]{"SETTINGS", "MONITOR"};

    public EngineScreen9002(StrategyData strategyData, AlphaVantageData alphaVantageData) {
        this.strategyData = strategyData;

        /* REGISTER TO RECEIVE UPDATE */
        this.strategyData.registerObserver(this);

        this.alphaVantageData = alphaVantageData;

        /* REGISTER TO RECEIVE UPDATE */
        this.alphaVantageData.registerObserver(this);

        update();
    }

    @Override
    public void init() {
        /* CLEAN PANEL */
        removeAll();

        /* DISPLAY COMPONENTS */
        int idx = 0;
        for (int i = 0; i < 3; i++){
            for (int j = 0; j < 3; j++){
                if (idx < infoPanels.length){
                    infoPanels[idx] = new InfoPanel(IP_types[idx], IP_texts[idx], IP_values[idx]);
                    infoPanels[idx].setBounds(20+(i*263), 20+j*113, 260, 110);
                    add(infoPanels[idx]);
                    idx++;
                }
            }
        }

        for (int i = 0; i< buttonPanels.length; i++){
            buttonPanels[i] = new ButtonPanel(EnumType.NO_ICON, BT_Header[i], BT_Description[i], BT_Target[i]);
            buttonPanels[i].setBounds(20, 359+i*113, 523, 110);
            add(buttonPanels[i]);
        }

        ImageLabel logoImg = new ImageLabel(state+".png");
        logoImg.setBounds(583, 380, 200, 200);
        logoImg.addMouseListener(mouseListener);
        add(logoImg);

        repaint();
    }

    @Override
    public void update() {
        /* CALL METHOD OF strategyData, alphaVantageData TO UPDATE IP_values AND IP_types */
        IP_values[0] = strategyData.getAsset();
        IP_values[2] = String.valueOf(strategyData.getAccuracy());
        IP_values[3] = strategyData.getTimescale();
        IP_values[4] = String.valueOf(strategyData.getMultiplier());
        IP_values[5] = strategyData.getOrder();
        IP_values[6] = strategyData.getTake_profit() +" %";
        IP_values[7] = strategyData.getStop_loss() +" %";

        if (alphaVantageData.getAPI_KEY() == null || alphaVantageData.getAPI_KEY().equals("")){
            IP_values[8] = "Inactive";
            IP_types[8] = EnumType.RED;
            apiKeyAvailable = false;
        }else if (alphaVantageData.getAPI_KEY().length() == 16){
            IP_values[8] = "Active";
            IP_types[8] = EnumType.GREEN;
            apiKeyAvailable = true;
        }

        /* DISPLAY UPDATES ON SCREEN */
        init();
    }

    public void startAndStop () {
        /* START ONLY IF API KEY IS AVAILABLE */
        if (!apiKeyAvailable){
            JOptionPane.showMessageDialog(getRootPane(), "APIs key missing !");
            return;
        }

        if (state.equals("stop")){
            if (JFrameBTWS.getInstance().start()){
                /* CHANGE STATUS AND DISPLAY NOTIFICATION */
                state = "start";
                display("ENGINE STARTED! Please do not switch off the computer and check the strategy settings.");

            }
        } else if (state.equals("start")){
            if (JFrameBTWS.getInstance().stop()){
                /* CHANGE STATUS AND DISPLAY NOTIFICATION */
                state = "stop";
                display("ENGINE STOPPED! You can turn off the computer and check the results of the strategy.");
            }
        }

        /* DISPLAY UPDATES ON SCREEN */
        init();
    }

    private class startAndStop extends MouseAdapter {
        /* MOUSE ADAPTER FOR BUTTON */
        @Override
        public void mouseClicked(MouseEvent e) {
            startAndStop();
        }
    }

    private void display (String message){
        /* DISPLAY NOTIFICATION ON WINDOWS COMPUTER */
        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                Image image = Toolkit.getDefaultToolkit().createImage("FAV-512.png");

                TrayIcon trayIcon = new TrayIcon(image, "BULLITT TWS");
                trayIcon.setImageAutoSize(true);
                trayIcon.setToolTip("BULLITT TWS");

                tray.add(trayIcon);
                trayIcon.displayMessage("BULLITT TWS ALERT", message, TrayIcon.MessageType.NONE);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("System tray not supported!");
        }
    }
}
