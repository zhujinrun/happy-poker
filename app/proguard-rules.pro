# Project-specific R8 rules can be added here when needed.

# Paho creates its logger and some network modules via reflection.
-keep class com.happy.poker.core.network.NoOpMqttLogger { public <init>(); *; }
-keep class org.eclipse.paho.client.mqttv3.logging.** { *; }
-keep class org.eclipse.paho.client.mqttv3.spi.** { *; }
-keep class org.eclipse.paho.client.mqttv3.internal.TCPNetworkModuleFactory { *; }
-keep class org.eclipse.paho.client.mqttv3.internal.SSLNetworkModuleFactory { *; }
-keep class org.eclipse.paho.client.mqttv3.internal.websocket.** { *; }
-dontwarn org.eclipse.paho.**
