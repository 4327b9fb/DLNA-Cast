# TransportFactory SPI interface — implementations loaded via ServiceLoader
-keep interface com.android.cast.dlna.core.transport.TransportFactory

# Keep all TransportFactory implementations (registered in META-INF/services)
-keep class * implements com.android.cast.dlna.core.transport.TransportFactory { <init>(); }