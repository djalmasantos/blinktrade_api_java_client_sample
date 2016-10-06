This is an example of how to use the <a href="https://blinktrade.com/docs/#getting-started">Blinktrade Websocket API</a> in Java.

The application demonstrates the following use cases:

- Websocket connection
- Protocol engine basics (logon, test request, heartbeat and JSON/FIX messaging)
- Order Book Management
- Security List Request and Reply processing
- Order List Request and Reply processing
- Balance Request
- MiniOMS (in-memory updated information of the user's alive orders)
- Demo TradingStrategy (send/cancel/replace orders with the price incremented or decremented by 0.01 to fight for a position on the top of the book, but never initiating the execution and respecting the target price parameter)

<b>Setup</b><br>
Java 8<br>
Maven 3.0<br>
Eclipse Neon (optional)<br>

<b>Dependencies</b><br>
jetty.websocket.websocket-client<br>
glassfish.javax.json<br>
jetty-util<br>

<b>Download, build and run from the command line</b><br>
```
$ wget https://github.com/djalmasantos/blinktrade_api_java_client_sample/archive/master.zip
$ unzip master.zip
$ cd blinktrade_api_java_client_sample-master
$ mvn clean package
$ mvn exec:java -Dexec.mainClass="com.blinktrade.api.websocket.client.sample.SimpleTradeClient" -Dexec.args="--help"
```
The program execution output will show the available program options and explain correct usage of the program.<br> 

<b>Is there a test environment to try out this sample app?</b><br>
Yes, <a href="https://testnet.blinktrade.com/">blinktrade tesnet exchange</a>.

<b>List of exchanges running the blinktrade platform</b><br>
- [chilebit](https://chilebit.net)
- [foxbit](https://foxbit.com.br)
- [VBTC](https://vbtc.vn)
- [surbitcoin](https://surbitcoin.com) 
- [urdubit](https://urdubit.com)
