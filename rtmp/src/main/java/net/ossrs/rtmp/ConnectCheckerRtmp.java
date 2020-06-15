package net.ossrs.rtmp;

import androidx.annotation.NonNull;

/**
 * Created by pedro on 25/01/17.
 */

public interface ConnectCheckerRtmp {

  void onConnectionSuccessRtmp();

  void onConnectionFailedRtmp(@NonNull String reason);

  void onNewBitrateRtmp(long bitrate);

  void onDisconnectRtmp();

  void onAuthErrorRtmp();

  void onAuthSuccessRtmp();
}
