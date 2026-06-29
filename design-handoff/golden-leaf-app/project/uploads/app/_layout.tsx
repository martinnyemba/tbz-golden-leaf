import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { Appearance, Text, TextInput, Alert, Pressable, StyleSheet } from 'react-native';
import { Stack, usePathname, useRouter } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import 'react-native-reanimated';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { useFonts } from 'expo-font';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect, useRef } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

import { Colors } from '@/constants/theme';
import { OfflineSyncManager } from '@/components/OfflineSyncManager';
import { FontFamily } from '@/constants/typography';
import { useColorScheme } from '@/hooks/use-color-scheme';
import { ensureOfflineDatabaseReady } from '@/lib/offline-db';
import {
  defineOfflineSyncTask,
  registerOfflineBackgroundFetch,
} from '@/lib/offline-background';
import { API_BASE_URL_KEY, extractErrorMessage, sanitizePortalBaseUrl } from '@/lib/inspection-storage';

// Tasks must be defined at module scope so the OS can dispatch them
// before any component has mounted.
defineOfflineSyncTask();

export const unstable_settings = {
  anchor: '(tabs)',
};

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const [loaded, error] = useFonts({});

  const router = useRouter();
  const lastRejectionAtRef = useRef(0);

  useEffect(() => {
    try {
      const TextAny = Text as any;
      const TextInputAny = TextInput as any;

      TextAny.defaultProps = TextAny.defaultProps || {};
      const prevTextStyle = TextAny.defaultProps.style;
      TextAny.defaultProps.style = [
        { fontFamily: FontFamily.serif },
        ...(Array.isArray(prevTextStyle) ? prevTextStyle : prevTextStyle ? [prevTextStyle] : []),
      ];

      TextInputAny.defaultProps = TextInputAny.defaultProps || {};
      const prevInputStyle = TextInputAny.defaultProps.style;
      TextInputAny.defaultProps.style = [
        { fontFamily: FontFamily.serif },
        ...(Array.isArray(prevInputStyle) ? prevInputStyle : prevInputStyle ? [prevInputStyle] : []),
      ];
    } catch {
    }

    if (loaded || error) {
      void ensureOfflineDatabaseReady();
      void registerOfflineBackgroundFetch();
      AsyncStorage.getItem(API_BASE_URL_KEY).then((existing) => {
        const current = sanitizePortalBaseUrl(existing ?? "");
        const prodDefault = sanitizePortalBaseUrl("https://trms.tbz.co.zm/");

        if (!__DEV__) {
          if (!current) void AsyncStorage.setItem(API_BASE_URL_KEY, prodDefault);
          return;
        }

        const hasCustom = Boolean(current && current !== prodDefault);
        if (hasCustom) return;

        const forced = process.env.EXPO_PUBLIC_TBZ_TRMS_BASE_URL || "http://127.0.0.1:8000/";
        void AsyncStorage.setItem(API_BASE_URL_KEY, sanitizePortalBaseUrl(forced));
      });
      AsyncStorage.multiGet(["tbz:onboarding_v2", "tbz:portalAccessToken:v1"]).then((vals) => {
        const onboarding = vals.find((v) => v[0] === "tbz:onboarding_v2")?.[1];
        const token = vals.find((v) => v[0] === "tbz:portalAccessToken:v1")?.[1];

        if (!onboarding) {
          router.replace("/onboarding");
        } else if (!token) {
          router.replace("/portal-login");
        }
        // Give the router an extra tick to navigate before dropping the splash screen to avoid flash
        setTimeout(() => {
          SplashScreen.hideAsync();
        }, 100);
      });
      
      AsyncStorage.getItem("tbz:theme").then((theme) => {
        if (theme === "dark" || theme === "light") {
          Appearance.setColorScheme(theme);
        }
      });
    }
  }, [loaded, error, router]);

  useEffect(() => {
    try {
      const tracking = require("promise/setimmediate/rejection-tracking");
      tracking.enable({
        allRejections: true,
        onUnhandled: (_id: unknown, err: unknown) => {
          const now = Date.now();
          if (now - lastRejectionAtRef.current < 2500) return;
          lastRejectionAtRef.current = now;
          const msg = err instanceof Error ? err.message : String(err ?? "Unexpected error");
          Alert.alert("Error", extractErrorMessage(msg) || "An unexpected error occurred.");
        },
      });
    } catch {
    }
  }, []);

  const colorScheme = useColorScheme();
  const theme = colorScheme === 'dark' ? 'dark' : 'light';
  const pathname = usePathname();
  
  if (!loaded && !error) {
    return null;
  }
  
  const navigationTheme = {
    ...(theme === 'dark' ? DarkTheme : DefaultTheme),
    colors: {
      ...(theme === 'dark' ? DarkTheme.colors : DefaultTheme.colors),
      primary: Colors[theme].primary,
      background: Colors[theme].background,
      card: Colors[theme].surface,
      text: Colors[theme].text,
      border: Colors[theme].border,
      notification: Colors[theme].accent,
    },
  };

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <ThemeProvider value={navigationTheme}>
        <Stack
          screenOptions={{
            animation: "slide_from_right",
            animationDuration: 250,
            headerStyle: { backgroundColor: Colors[theme].primary },
            headerTintColor: Colors[theme].surface,
            headerShadowVisible: false,
            headerTitleAlign: "left",
            headerTitleStyle: { fontSize: 22, fontFamily: FontFamily.serif, fontWeight: "600", color: Colors[theme].surface },
            headerLeft: ({ canGoBack }) =>
              canGoBack ? (
                <Pressable onPress={() => router.back()} style={{ paddingHorizontal: 16, paddingVertical: 8 }} hitSlop={10}>
                  <MaterialIcons name="arrow-back" size={24} color={Colors[theme].surface} />
                </Pressable>
              ) : null,
          }}
        >
          <Stack.Screen name="(tabs)" options={{ headerShown: false, animation: "fade" }} />
          <Stack.Screen name="search" options={{ headerShown: false, animation: "fade" }} />
          <Stack.Screen name="portal-login" options={{ title: "Portal Login" }} />
          <Stack.Screen name="login-2fa" options={{ title: "Two-Factor Login" }} />
          <Stack.Screen name="change-password" options={{ headerShown: false }} />
          <Stack.Screen name="two-factor-auth" options={{ headerShown: false }} />
          <Stack.Screen name="about" options={{ headerShown: false }} />
          <Stack.Screen name="terms" options={{ headerShown: false }} />
          <Stack.Screen name="privacy" options={{ headerShown: false }} />
          <Stack.Screen name="guidelines" options={{ headerShown: false }} />
          <Stack.Screen name="permit-request" options={{ title: "Permit Request" }} />
          <Stack.Screen name="permit-validate" options={{ title: "Validate Permit" }} />
          <Stack.Screen name="permit-group" options={{ title: "Group Permits" }} />
          <Stack.Screen name="permit-list" options={{ title: "Permit List" }} />
          <Stack.Screen name="permit-detail" options={{ title: "Permit Details" }} />
          <Stack.Screen name="modal" options={{ presentation: 'modal', title: 'Modal' }} />
          <Stack.Screen name="menu" options={{ presentation: 'modal', headerShown: false }} />
          <Stack.Screen name="notifications" options={{ presentation: 'modal', headerShown: false }} />
          <Stack.Screen name="profile" options={{ title: "Profile" }} />
          <Stack.Screen name="onboarding" options={{ headerShown: false }} />
          <Stack.Screen name="local-registrations" options={{ presentation: "modal", title: "Saved Registrations" }} />
          <Stack.Screen name="pending-sales" options={{ headerShown: false }} />
          <Stack.Screen name="edit-pending-sale" options={{ headerShown: false }} />
          <Stack.Screen name="sales" options={{ headerShown: false }} />
          <Stack.Screen name="sync-settings" options={{ headerShown: false }} />
        </Stack>
        <OfflineSyncManager />
        <StatusBar
          style="light"
          translucent={false}
          backgroundColor={Colors[theme].primary}
        />
      </ThemeProvider>
    </GestureHandlerRootView>
  );
}

const styles = StyleSheet.create({
});
