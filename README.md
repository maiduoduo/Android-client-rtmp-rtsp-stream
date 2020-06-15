# rtmp-rtsp-stream-client-java

## 一.RTSP框架似乎很少

试着搭建RTSP直播的框架，发现可参考RTSP的直播框架很少。
 一开始以为live55可以的，但最后发现这个框架只支持播放本地文件，虽然可以定制，但是明显有点麻烦。
 后来又看vlc，发现搭建都是用图形界面的，而且推流的资料很少。
 还有苹果的Darwin，资料更是少得可怜。
 主要是现在貌似直播都是用RTMP，RTSP的框架基本没有

## 二.最终结果

考虑到开源编译定制，后面选择了下面的搭配：
 服务端：[https://github.com/EasyDarwin/EasyDarwin](https://links.jianshu.com/go?to=https%3A%2F%2Fgithub.com%2FEasyDarwin%2FEasyDarwin)
 推流端：[https://github.com/maiduoduo/Android-client-rtmp-rtsp-stream](https://github.com/maiduoduo/Android-client-rtmp-rtsp-streama)
 客户端：VLC




[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-rtmp%20rtsp%20stream%20client%20java-green.svg?style=true)](https://android-arsenal.com/details/1/5333)
[![Release](https://jitpack.io/v/pedroSG94/rtmp-rtsp-stream-client-java.svg)](https://jitpack.io/#pedroSG94/rtmp-rtsp-stream-client-java)

Library for stream in RTMP and RTSP. All code in Java.

If you need a player see this project:

https://github.com/pedroSG94/vlc-example-streamplayer

## Wiki

https://github.com/pedroSG94/rtmp-rtsp-stream-client-java/wiki

## Permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<!--Optional for play store-->
<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
```

## Compile

To use this library in your project with gradle add this to your build.gradle:

```gradle
allprojects {
  repositories {
    maven { url 'https://jitpack.io' }
  }
}
dependencies {
  implementation 'com.github.pedroSG94.rtmp-rtsp-stream-client-java:rtplibrary:1.8.3'
}

```

## Features:

- [x] Android min API 16.
- [x] Support [camera1](https://developer.android.com/reference/android/hardware/Camera.html) and [camera2](https://developer.android.com/reference/android/hardware/camera2/package-summary.html) API
- [x] Encoder type buffer to buffer.
- [x] Encoder type surface to buffer.
- [x] RTMP/RTSP auth.
- [x] Audio noise suppressor.
- [x] Audio echo cancellation.
- [x] Disable/Enable video and audio while streaming.
- [x] Switch camera while streaming.
- [x] Change video bitrate while streaming (API 19+).
- [X] Get upload bandwidth used.
- [X] Record MP4 file while streaming (API 18+).
- [x] H264, H265 and AAC hardware encoding.
- [x] Force H264 and AAC Codec hardware/software encoding (Not recommended).
- [x] RTSP TCP/UDP.
- [x] Stream from video files like mp4, webm, etc (Limited by device decoders). [More info](https://github.com/pedroSG94/rtmp-rtsp-stream-client-java/wiki/Stream-from-file)
- [x] Stream device display(API 21+).
- [X] Set Image, Gif or Text to stream on real time.
- [X] OpenGL real time filters. [More info](https://github.com/pedroSG94/rtmp-rtsp-stream-client-java/wiki/Real-time-filters)
- [X] RTMPS and RTSPS
- [X] RTSP H265 support (Waiting FLV official packetization to add RTMP support).

## Other related projects:

https://github.com/pedroSG94/RTSP-Server

https://github.com/pedroSG94/AndroidReStreamer

https://github.com/pedroSG94/Stream-USB-test

## Use example:

This code is a basic example.
I recommend you go to Activities in app module and see all examples.

### RTMP:

```java

//default

//create builder
RtmpCamera1 rtmpCamera1 = new RtmpCamera1(surfaceView, connectCheckerRtmp);
//start stream
if (rtmpCamera1.prepareAudio() && rtmpCamera1.prepareVideo()) {
  rtmpCamera1.startStream("rtmp://yourEndPoint");
} else {
 /**This device cant init encoders, this could be for 2 reasons: The encoder selected doesnt support any configuration setted or your device hasnt a H264 or AAC encoder (in this case you can see log error valid encoder not found)*/
}
//stop stream
rtmpCamera1.stopStream();

//with params

//create builder
RtmpCamera1 rtmpCamera1 = new RtmpCamera1(surfaceView, connectCheckerRtmp);
//start stream
if (rtmpCamera1.prepareAudio(int bitrate, int sampleRate, boolean isStereo, boolean echoCanceler,
      boolean noiseSuppressor) && rtmpCamera1.prepareVideo(int width, int height, int fps, int bitrate, boolean hardwareRotation, int rotation)) {
  rtmpCamera1.startStream("rtmp://yourEndPoint");
} else {
 /**This device cant init encoders, this could be for 2 reasons: The encoder selected doesnt support any configuration setted or your device hasnt a H264 or AAC encoder (in this case you can see log error valid encoder not found)*/
}
//stop stream
rtmpCamera1.stopStream();

```

### RTSP:

```java

//default

//create builder
//by default TCP protocol.
RtspCamera1 rtspCamera1 = new RtspCamera1(surfaceView, connectCheckerRtsp);
//start stream
if (rtspCamera1.prepareAudio() && rtspCamera1.prepareVideo()) {
  rtspCamera1.startStream("rtsp://yourEndPoint");
} else {
 /**This device cant init encoders, this could be for 2 reasons: The encoder selected doesnt support any configuration setted or your device hasnt a H264 or AAC encoder (in this case you can see log error valid encoder not found)*/
}
//stop stream
rtspCamera1.stopStream();

//with params

//create builder
RtspCamera1 rtspCamera1 = new RtspCamera1(surfaceView, connectCheckerRtsp);
rtspCamera1.setProtocol(protocol);
//start stream
if (rtspCamera1.prepareAudio(int bitrate, int sampleRate, boolean isStereo, boolean echoCanceler,
      boolean noiseSuppressor) && rtspCamera1.prepareVideo(int width, int height, int fps, int bitrate, boolean hardwareRotation, int rotation)) {
  rtspCamera1.startStream("rtsp://yourEndPoint");
} else {
 /**This device cant init encoders, this could be for 2 reasons: The encoder selected doesnt support any configuration setted or your device hasnt a H264 or AAC encoder (in this case you can see log error valid encoder not found)*/
}
//stop stream
rtspCamera1.stopStream();

```

#### 来源于

[pedroSG94](https://links.jianshu.com/go?to=https%3A%2F%2Fgithub.com%2FpedroSG94%2Frtmp-rtsp-stream-client-java)
