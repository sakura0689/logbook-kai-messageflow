package logbook.webClient;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import logbook.config.LogBookKaiMessageFlowConfig;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

public class WebClientConfig {
    
    /** 
     * 航海日誌改をEndPointにする
     * なので、Proxy設定とEndPoint設定はイコールとなる
     * 
     * 航海日誌改がどこにProxyするか設定はHeaderのHost,Origin,Proxy-Connectionの3つ
     * */
    private static final String PROXY_HOST = "localhost";
    private static final int PROXY_PORT = LogBookKaiMessageFlowConfig.getInstance().getKoukainissikaiPort();
    private static final String BASE_URL = "http://localhost:" + PROXY_PORT;
    
    public static WebClient createCustomWebClient() {
                
        // バッファサイズを拡張（32MB）
        int bufferSize = 32 * 1024 * 1024;

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(bufferSize))
                .build();

        HttpClient httpClient = HttpClient.create()
                .proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
                        .host(PROXY_HOST)
                        .port(PROXY_PORT));
                //.wiretap(true); //debug mode
        
        // ReactorClientHttpConnectorを作成
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        
        
        return WebClient.builder()
                .baseUrl(BASE_URL)
                .exchangeStrategies(strategies)
                .clientConnector(connector)
                .build();
    }
}
