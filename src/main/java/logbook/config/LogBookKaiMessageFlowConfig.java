package logbook.config;

public class LogBookKaiMessageFlowConfig {
    
    private static LogBookKaiMessageFlowConfig instance;

    /** 航海日誌改デフォルトポート 8888 */
    private int koukainissikaiPort = 8888;
    /** 航海日誌改MessegeFlowデフォルトポート 8890 */
    private int koukainissikaiMessageFlowPort = 8890;
    
    private LogBookKaiMessageFlowConfig() {}

    public static LogBookKaiMessageFlowConfig getInstance() {
        if (instance == null) {
            instance = new LogBookKaiMessageFlowConfig();
        }
        return instance;
    }
    
    public void setKoukainissikaiPort(int koukainissikaiPort) {
        this.koukainissikaiPort = koukainissikaiPort;
    }
    
    public void setKoukainissikaiMessageFlowPort(int koukainissikaiMessageFlowPort) {
        this.koukainissikaiMessageFlowPort = koukainissikaiMessageFlowPort;
    }

    /**
     * 航海日誌改のポート番号を返却します
     */
    public int getKoukainissikaiPort() {
        return this.koukainissikaiPort;
    }
    
    /**
     * サーバー機能のOrigin情報を返却します
     * @return
     * 
     * @see logbook.server.ServerController
     */
    public String getKoukainissikaiMessageFlowOrigin() {
        return new StringBuilder().append("http://localhost:").append(this.koukainissikaiMessageFlowPort).toString();
    }
    
    /**
     * サーバー機能のHost情報を返却します
     * @return
     * 
     * @see logbook.server.ServerController
     */
    public String getKoukainissikaiMessageFlowHost() {
        return new StringBuilder().append("localhost:").append(this.koukainissikaiMessageFlowPort).toString();
    }
}
