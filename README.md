# DLNA-Cast

[![Download](https://jitpack.io/v/4327b9fb/DLNA-Cast.svg)](https://jitpack.io/#4327b9fb/DLNA-Cast)

基于 [jUPnP](https://github.com/jupnp/jupnp) 封装的 Android DLNA 投屏库，源自 [devin1014/DLNA-Cast](https://github.com/devin1014/DLNA-Cast)

---

## 功能

- **DMC** — 移动端设备发现与投屏控制
- **DMR** — 电视端播放器
- **DMS** — 服务端内容共享

---

## 引入

**1. 添加 JitPack 仓库**

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

**2. 添加依赖**

```groovy
implementation 'com.github.4327b9fb.DLNA-Cast:dmc:v3.0.0'
implementation 'com.github.4327b9fb.DLNA-Cast:dmr:v3.0.0'
implementation 'com.github.4327b9fb.DLNA-Cast:dms:v3.0.0'
```

---

## 传输层

DMC/DMR/DMS 默认依赖 `transport-jetty`，开箱即用。实际 minSdk 由所选传输层决定：使用默认 jetty 时为 26，切换 transport 后以对应模块为准。

| 传输层 | minSdk | 说明 |
|-------|--------|------|
| `transport-jetty` | 26 | 默认，Android 8.0+ |
| `transport-okhttp3` | 21 | Android 5.0+ |
| `transport-okhttp4` | 24 | Android 7.0+ |

**切换方式：**

```groovy
implementation('com.github.4327b9fb.DLNA-Cast:dmc:v3.0.0') {
    exclude module: 'transport-jetty'
}
implementation 'com.github.4327b9fb.DLNA-Cast:transport-okhttp3:v3.0.0'
// 或
implementation 'com.github.4327b9fb.DLNA-Cast:transport-okhttp4:v3.0.0'
```

也可以实现 `TransportFactory` 接口自定义传输层，并在 `META-INF/services/` 下注册。

---

## 使用

### 权限声明

```xml
<!-- jUPnP required -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
    <!-- WiFi scanning on Android 6+ requires location permissions -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### 服务声明

```xml
<service android:name="com.android.cast.dlna.dmc.DLNACastService" />
<service android:name="com.android.cast.dlna.dmr.DLNARendererService" />
<service android:name="com.android.cast.dlna.dms.DLNAContentService" />
```

### 绑定服务

```kotlin
override fun onStart() {
    DLNACastManager.bindCastService(this)
}

override fun onStop() {
    DLNACastManager.unbindCastService(this)
}
```

绑定后自动搜索设备，也可手动搜索：

```kotlin
DLNACastManager.search()
```

### 监听设备

```kotlin
DLNACastManager.registerDeviceListener(listener)
DLNACastManager.unregisterDeviceListener(listener)
```

> `OnDeviceRegistryListener` 回调始终在**主线程**调用。

### 控制设备

```kotlin
val deviceControl: DeviceControl = DLNACastManager.connectDevice(device, callback)
```

**播放控制**

| 方法 | 说明 |
|------|------|
| `setAVTransportURI(uri, title, callback)` | 投射当前视频 |
| `setNextAVTransportURI(uri, title, callback)` | 投射下一个视频（部分设备不支持） |
| `play(speed, callback)` | 播放 |
| `pause(callback)` | 暂停 |
| `stop(callback)` | 停止 |
| `seek(milliseconds, callback)` | 跳转到指定播放位置 |
| `next(callback)` | 下一个 |
| `previous(callback)` | 上一个 |

**状态查询**

| 方法 | 说明 |
|------|------|
| `getPositionInfo(callback)` | 获取播放进度 |
| `getMediaInfo(callback)` | 获取视频信息 |
| `getTransportInfo(callback)` | 获取播放状态 |

**音量控制**

| 方法 | 说明 |
|------|------|
| `setVolume(volume, callback)` | 设置音量 |
| `getVolume(callback)` | 获取音量 |
| `setMute(mute, callback)` | 设置静音 |
| `getMute(callback)` | 获取静音状态 |

**内容浏览**

| 方法 | 说明 |
|------|------|
| `browse(objectId, flag, filter, firstResult, maxResults, callback)` | 浏览内容目录 |
| `search(containerId, searchCriteria, filter, firstResult, maxResults, callback)` | 搜索内容 |

每个操作都有 `ServiceActionCallback` 回调监听成功/失败。