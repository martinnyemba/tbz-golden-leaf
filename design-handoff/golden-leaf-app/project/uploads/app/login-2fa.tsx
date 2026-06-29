import React, { useState } from "react";
import { StyleSheet, View, Pressable, Platform, Alert } from "react-native";
import { useLocalSearchParams, useRouter } from "expo-router";
import AsyncStorage from "@react-native-async-storage/async-storage";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton, TextField } from "@/components/ui/form-controls";
import { LeafLoader } from "@/components/LeafLoader";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";
import {
  AUTH_ACCESS_KEY,
  AUTH_REFRESH_KEY,
  computeRetryAfterSeconds,
  getLoginCooldownUntil,
  setLoginCooldownUntil,
} from "@/lib/inspection-storage";

export default function Login2FAScreen() {
  const router = useRouter();
  const params = useLocalSearchParams();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";

  const { email, password, baseUrl, returnTo } = params as { 
    email?: string; 
    password?: string; 
    baseUrl?: string;
    returnTo?: string;
  };

  const [otpCode, setOtpCode] = useState("");
  const [loading, setLoading] = useState(false);

  const handleVerify = async () => {
    if (!otpCode || otpCode.length < 6) {
      Alert.alert("Missing Token", "Please enter your 6-digit Authenticator code.");
      return;
    }

    if (!email || !password || !baseUrl) {
      Alert.alert("Session Expired", "Authentication details lost. Please log in again.");
      router.back();
      return;
    }

    const cooldownUntil = await getLoginCooldownUntil();
    if (cooldownUntil > Date.now()) {
      const remaining = Math.max(1, Math.ceil((cooldownUntil - Date.now()) / 1000));
      Alert.alert("Login failed", `Too many requests. Try again in ${remaining}s.`);
      return;
    }

    setLoading(true);
    try {
      const loginEmail = email.trim();
      const loginPassword = password;
      const bodyArgs1: any = { email: loginEmail, password: loginPassword, otp_code: otpCode };
      const bodyArgs2: any = { username: loginEmail, password: loginPassword, otp_code: otpCode };

      let resp = await fetch(`${baseUrl}/api/v1/auth/login/`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(bodyArgs1),
      });

      if (!resp.ok && (resp.status === 400 || resp.status === 401)) {
        resp = await fetch(`${baseUrl}/api/v1/auth/login/`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(bodyArgs2),
        });
      }

      if (resp.status === 429) {
        const retrySeconds = computeRetryAfterSeconds(resp.headers) ?? 60;
        const until = Date.now() + retrySeconds * 1000;
        await setLoginCooldownUntil(until);
        Alert.alert("Login failed", `Too many requests. Please slow down.\n\nTry again in ${retrySeconds}s.`);
        return;
      }

      if (!resp.ok) {
        const raw = await resp.text();
        let errMsg = "Network request failed";
        try {
          const payload = JSON.parse(raw);
          errMsg = payload?.otp_code || payload?.error?.message || payload?.detail || payload?.message || raw.trim().slice(0, 400);
        } catch {
          errMsg = raw.trim() ? raw.slice(0, 400) : "No response body.";
        }
        Alert.alert("Authentication Failed", errMsg);
        return;
      }

      const payload = (await resp.json()) as { access?: string; refresh?: string };
      const nextAccess = payload.access ?? "";
      const nextRefresh = payload.refresh ?? "";
      
      if (!nextAccess || !nextRefresh) {
        Alert.alert("Login failed", "Portal did not return token payload.");
        return;
      }

      await AsyncStorage.setItem(AUTH_ACCESS_KEY, nextAccess);
      await AsyncStorage.setItem(AUTH_REFRESH_KEY, nextRefresh);
      await setLoginCooldownUntil(0);

      if (returnTo && returnTo.startsWith("/")) {
        router.replace(returnTo as any);
      } else if (returnTo === "registration") {
        router.back();
      } else {
        await AsyncStorage.setItem("tbz:onboarding_v2", "true");
        router.replace("/");
      }

    } catch (err) {
      const msg = err instanceof Error ? err.message : typeof err === "string" ? err : JSON.stringify(err);
      Alert.alert("Login failed", msg || "Network error while completing 2FA.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <ThemedView style={styles.container}>
      <View style={[styles.header, { backgroundColor: Colors[theme].primary }]}>
        <Pressable onPress={() => router.back()} style={styles.backButton} disabled={loading}>
          <MaterialIcons name="chevron-left" size={32} color={Colors[theme].surface} />
        </Pressable>
        <ThemedText style={[styles.headerTitle, { color: Colors[theme].surface }]}>
          Two-Factor Authentication
        </ThemedText>
        <View style={{ width: 48 }} />
      </View>

      <View style={styles.content}>
        <View style={styles.iconBox}>
           <MaterialIcons name="lock-clock" size={64} color={Colors[theme].primary} />
        </View>
        <ThemedText style={styles.title}>Authenticator Code</ThemedText>
        <ThemedText style={styles.subtitle}>
          Your account is secured with Two-Factor Authentication. Please open your authenticator app and enter the 6-digit verification token below.
        </ThemedText>

        <View style={styles.cardBlock}>
          <TextField 
            label="Verification PIN"
            placeholder="000000"
            value={otpCode}
            onChangeText={setOtpCode}
            keyboardType="number-pad"
          />
          <View style={{ marginTop: 24 }}>
             {loading ? <LeafLoader size={26} /> : <PrimaryButton title="Verify & Continue" onPress={handleVerify} />}
          </View>
        </View>
      </View>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingTop: Platform.OS === "android" ? 40 : 60, paddingBottom: 16, paddingHorizontal: 8 },
  backButton: { padding: 8, width: 48, alignItems: "center" },
  headerTitle: { fontSize: 20, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular },
  content: { padding: 24, paddingBottom: 100 },
  iconBox: { alignItems: "center", marginBottom: 24, marginTop: 16 },
  title: { fontSize: 26, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, textAlign: "center", marginVertical: 8 },
  subtitle: { fontSize: 16, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular, textAlign: "center", color: '#666', lineHeight: 22, paddingHorizontal: 12, marginBottom: 32 },
  cardBlock: { backgroundColor: 'rgba(0,0,0,0.03)', padding: 24, borderRadius: 16 },
});
