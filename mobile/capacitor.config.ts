import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "org.upscayl.mobile",
  appName: "Upscayl",
  webDir: "www",
  bundledWebRuntime: false,
  android: {
    allowMixedContent: false
  },
  plugins: {
    SplashScreen: {
      launchAutoHide: true
    }
  }
};

export default config;
