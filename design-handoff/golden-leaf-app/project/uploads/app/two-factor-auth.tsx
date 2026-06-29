import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Alert, Pressable, ScrollView, StyleSheet, View } from "react-native";
import { Stack, useRouter } from "expo-router";
import AsyncStorage from "@react-native-async-storage/async-storage";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import * as WebBrowser from "expo-web-browser";
import QRCode from "react-native-qrcode-svg";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton, TextField } from "@/components/ui/form-controls";
import { LeafLoader } from "@/components/LeafLoader";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { apiErrorUi, apiFetchJson, API_BASE_URL_KEY, runtimePortalBaseUrl } from "@/lib/inspection-storage";

export default function TwoFactorAuthScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";

  const [booting, setBooting] = useState(true);
  const [busy, setBusy] = useState(false);

  const [is2FAEnabled, setIs2FAEnabled] = useState(false);
  const [phase, setPhase] = useState<"idle" | "setup" | "disable">("idle");

  const [setupSecret, setSetupSecret] = useState("");
  const [setupUri, setSetupUri] = useState("");
  const [otpCode, setOtpCode] = useState("");
  const [recoveryCodes, setRecoveryCodes] = useState<string[]>([]);
  const [apiNotSupported, setApiNotSupported] = useState(false);

  useEffect(() => {
    (async () => {
      setBooting(true);
      try {
        const resp = await apiFetchJson("/api/v1/auth/me/", { method: "GET" });
        if (resp.ok) setIs2FAEnabled(Boolean((JSON.parse(resp.body) as any)?.two_factor_enabled));
      } catch {}
      setBooting(false);
    })();
  }, [router]);

  const refreshStatus = useCallback(async () => {
    const resp = await apiFetchJson("/api/v1/auth/me/", { method: "GET" });
    if (!resp.ok) return false;
    const next = Boolean((JSON.parse(resp.body) as any)?.two_factor_enabled);
    setIs2FAEnabled(next);
    return true;
  }, []);

  const openPortalSettings = useCallback(async () => {
    const savedBase = (await AsyncStorage.getItem(API_BASE_URL_KEY)) ?? "";
    const base = runtimePortalBaseUrl(savedBase);
    if (!base) {
      Alert.alert("Not connected", "Please connect to the portal first.");
      return;
    }
    await WebBrowser.openBrowserAsync(`${base}/accounts/settings/`);
  }, []);

  const looksLikeNotFoundHtml = useCallback((body: string) => {
    const raw = String(body ?? "").trim();
    if (!raw) return false;
    if (!raw.startsWith("<")) return false;
    const lower = raw.toLowerCase();
    return lower.includes("page not found") || lower.includes("<title>page not found");
  }, []);

  const postWithFallback = useCallback(
    async (paths: string[], init: RequestInit) => {
      let last = { ok: false, status: 0, body: "" } as any;
      for (const p of paths) {
        const resp = await apiFetchJson(p, init);
        last = resp;
        if (resp.ok) return resp;
        if (resp.status !== 404 && !looksLikeNotFoundHtml(resp.body)) return resp;
      }
      return last as { ok: boolean; status: number; body: string };
    },
    [looksLikeNotFoundHtml],
  );

  const handleStartSetup = async () => {
    setBusy(true);
    try {
      const resp = await postWithFallback(
        [
          "/api/v1/auth/2fa/setup/",
          "/api/v1/auth/2fa/setup",
          "/api/v1/auth/two-factor/setup/",
          "/api/v1/auth/two-factor/setup",
        ],
        { method: "POST" },
      );
      if (!resp.ok) {
        if (resp.status === 404 || looksLikeNotFoundHtml(resp.body)) {
          setApiNotSupported(true);
          Alert.alert(
            "Not available",
            "This portal does not expose Two-Factor Authentication setup via API. Use the web portal settings to enable it.",
            [{ text: "Open Settings", onPress: () => void openPortalSettings() }, { text: "OK" }],
          );
          return;
        }
        const ui = apiErrorUi(resp.status, resp.body);
        if (ui.kind === "login_required") {
          Alert.alert(ui.title, ui.message, [
            { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: "/two-factor-auth" } } as any) },
            { text: "OK" },
          ]);
        } else {
          Alert.alert(ui.title, ui.message);
        }
        return;
      }
      const data = JSON.parse(resp.body) as any;
      setSetupSecret(String(data?.secret ?? ""));
      setSetupUri(String(data?.uri ?? ""));
      setRecoveryCodes(Array.isArray(data?.recovery_codes) ? data.recovery_codes.map(String) : []);
      setPhase("setup");
    } catch {
      Alert.alert("Error", "Unable to start 2FA setup.");
    } finally {
      setBusy(false);
    }
  };

  const handleConfirmEnable = async () => {
    if (!otpCode || otpCode.length < 6) {
      Alert.alert("Missing OTP", "Please enter the 6-digit code from your authenticator app.");
      return;
    }
    setBusy(true);
    try {
      const resp = await postWithFallback(
        [
          "/api/v1/auth/2fa/enable/",
          "/api/v1/auth/2fa/enable",
          "/api/v1/auth/two-factor/enable/",
          "/api/v1/auth/two-factor/enable",
        ],
        { method: "POST", body: JSON.stringify({ secret: setupSecret, code: otpCode.trim() }) },
      );
      if (!resp.ok) {
        if (resp.status === 404 || looksLikeNotFoundHtml(resp.body)) {
          setApiNotSupported(true);
          Alert.alert(
            "Not available",
            "This portal does not expose Two-Factor Authentication enable via API. Use the web portal settings to enable it.",
            [{ text: "Open Settings", onPress: () => void openPortalSettings() }, { text: "OK" }],
          );
          return;
        }
        const ui = apiErrorUi(resp.status, resp.body);
        if (ui.kind === "login_required") {
          Alert.alert(ui.title, ui.message, [
            { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: "/two-factor-auth" } } as any) },
            { text: "OK" },
          ]);
        } else {
          Alert.alert(ui.title, ui.message);
        }
        return;
      }
      try {
        const payload = JSON.parse(resp.body) as any;
        const codes = payload?.recovery_codes ?? payload?.backup_codes ?? null;
        if (Array.isArray(codes) && codes.length > 0) setRecoveryCodes(codes.map(String));
      } catch {}

      await refreshStatus();
      setPhase("idle");
      setOtpCode("");
      Alert.alert("Success", "Two-Factor Authentication enabled.");
    } catch {
      Alert.alert("Error", "Unable to verify OTP.");
    } finally {
      setBusy(false);
    }
  };

  const handleConfirmDisable = async () => {
    if (!otpCode || otpCode.length < 6) {
      Alert.alert("Missing OTP", "Please enter your current 6-digit code.");
      return;
    }
    setBusy(true);
    try {
      const resp = await postWithFallback(
        [
          "/api/v1/auth/2fa/disable/",
          "/api/v1/auth/2fa/disable",
          "/api/v1/auth/two-factor/disable/",
          "/api/v1/auth/two-factor/disable",
        ],
        { method: "POST", body: JSON.stringify({ code: otpCode.trim() }) },
      );
      if (!resp.ok) {
        if (resp.status === 404 || looksLikeNotFoundHtml(resp.body)) {
          setApiNotSupported(true);
          Alert.alert(
            "Not available",
            "This portal does not expose Two-Factor Authentication disable via API. Use the web portal settings to disable it.",
            [{ text: "Open Settings", onPress: () => void openPortalSettings() }, { text: "OK" }],
          );
          return;
        }
        const ui = apiErrorUi(resp.status, resp.body);
        if (ui.kind === "login_required") {
          Alert.alert(ui.title, ui.message, [
            { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: "/two-factor-auth" } } as any) },
            { text: "OK" },
          ]);
        } else {
          Alert.alert(ui.title, ui.message);
        }
        return;
      }
      await refreshStatus();
      setPhase("idle");
      setOtpCode("");
      Alert.alert("Success", "Two-Factor Authentication disabled.");
    } catch {
      Alert.alert("Error", "Unable to disable 2FA.");
    } finally {
      setBusy(false);
    }
  };

  const headerTitle = useMemo(() => "Two-Factor Authentication", []);

  if (booting) {
    return (
      <ThemedView style={[styles.container, styles.center]}>
        <Stack.Screen
          options={{
            headerShown: true,
            title: headerTitle,
            headerStyle: { backgroundColor: Colors[theme].primary },
            headerTitleStyle: { fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: Colors[theme].surface },
            headerTintColor: Colors[theme].surface,
          }}
        />
        <LeafLoader size={42} />
      </ThemedView>
    );
  }

  return (
    <ThemedView style={styles.container}>
      <Stack.Screen
        options={{
          headerShown: true,
          title: headerTitle,
          headerStyle: { backgroundColor: Colors[theme].primary },
          headerTitleStyle: { fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: Colors[theme].surface },
          headerTintColor: Colors[theme].surface,
        }}
      />

      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.statusCard}>
          <View style={styles.statusIconRow}>
            <MaterialIcons name={is2FAEnabled ? "verified-user" : "gpp-bad"} size={42} color={is2FAEnabled ? "#10B981" : "#EF4444"} />
            <View style={{ flex: 1 }}>
              <ThemedText style={styles.statusTitle}>{is2FAEnabled ? "Two-Factor Authentication is ON" : "Two-Factor Authentication is OFF"}</ThemedText>
              <ThemedText style={[styles.statusSubtitle, { color: Colors[theme].muted }]}>
                {is2FAEnabled
                  ? "Sign-in requires a time-based code from your authenticator app."
                  : "Enable an authenticator app to add an extra layer of security to your account."}
              </ThemedText>
            </View>
          </View>
          <View style={styles.statusActions}>
            {!is2FAEnabled ? (
              <PrimaryButton title={busy ? "Loading…" : "Enable Authenticator"} onPress={handleStartSetup} disabled={busy} loading={busy} />
            ) : (
              <Pressable style={styles.dangerOutline} onPress={() => setPhase("disable")} disabled={busy}>
                <MaterialIcons name="no-encryption" size={18} color="#EF4444" />
                <ThemedText style={styles.dangerOutlineText}>Disable 2FA</ThemedText>
              </Pressable>
            )}
            {apiNotSupported ? (
              <View style={{ marginTop: 10 }}>
                <PrimaryButton title="Open Web Portal Settings" onPress={openPortalSettings} />
              </View>
            ) : null}
          </View>
        </View>

        {phase === "setup" ? (
          <View style={styles.panel}>
            <ThemedText style={styles.panelTitle}>Authenticator Setup</ThemedText>
            <ThemedText style={[styles.panelText, { color: Colors[theme].muted }]}>
              Scan the QR code with Google Authenticator, Microsoft Authenticator, or Authy.
            </ThemedText>

            <View style={styles.qrWrapper}>
              {setupUri ? <QRCode value={setupUri} size={220} /> : <LeafLoader size={34} />}
            </View>

            <ThemedText style={[styles.panelText, { color: Colors[theme].muted }]}>Manual setup key</ThemedText>
            <View style={styles.secretBox}>
              <ThemedText style={styles.secretValue}>{setupSecret || "—"}</ThemedText>
            </View>

            <View style={{ height: 14 }} />

            <ThemedText style={styles.panelTitle}>Verify Code</ThemedText>
            <ThemedText style={[styles.panelText, { color: Colors[theme].muted }]}>Enter the 6-digit code from your authenticator app.</ThemedText>
            <TextField label="Verification code" placeholder="000000" value={otpCode} onChangeText={setOtpCode} keyboardType="number-pad" />

            <View style={{ marginTop: 14, gap: 10 }}>
              <PrimaryButton title={busy ? "Enabling…" : "Enable 2FA"} onPress={handleConfirmEnable} disabled={busy} loading={busy} />
              <Pressable onPress={() => setPhase("idle")} disabled={busy} style={styles.secondaryBtn}>
                <ThemedText style={styles.secondaryBtnText}>Cancel</ThemedText>
              </Pressable>
            </View>

            {recoveryCodes.length > 0 ? (
              <View style={{ marginTop: 18 }}>
                <ThemedText style={styles.panelTitle}>Recovery Codes</ThemedText>
                <ThemedText style={[styles.panelText, { color: Colors[theme].muted }]}>
                  Save these codes in a secure place. Each code can be used once.
                </ThemedText>
                <View style={styles.codesBox}>
                  {recoveryCodes.map((c) => (
                    <ThemedText key={c} style={styles.codeItem}>
                      {c}
                    </ThemedText>
                  ))}
                </View>
              </View>
            ) : null}
          </View>
        ) : null}

        {phase === "disable" ? (
          <View style={styles.panel}>
            <ThemedText style={[styles.panelTitle, { color: "#EF4444" }]}>Disable Two-Factor Authentication</ThemedText>
            <ThemedText style={[styles.panelText, { color: Colors[theme].muted }]}>
              Enter your current 6-digit code to confirm disabling 2FA.
            </ThemedText>
            <TextField label="Verification code" placeholder="000000" value={otpCode} onChangeText={setOtpCode} keyboardType="number-pad" />
            <View style={{ marginTop: 14, gap: 10 }}>
              <Pressable style={styles.dangerFilled} onPress={handleConfirmDisable} disabled={busy}>
                {busy ? <LeafLoader size={18} color="#FFFFFF" /> : <MaterialIcons name="delete-forever" size={18} color="#FFFFFF" />}
                <ThemedText style={styles.dangerFilledText}>{busy ? "Disabling…" : "Disable 2FA"}</ThemedText>
              </Pressable>
              <Pressable onPress={() => setPhase("idle")} disabled={busy} style={styles.secondaryBtn}>
                <ThemedText style={styles.secondaryBtnText}>Cancel</ThemedText>
              </Pressable>
            </View>
          </View>
        ) : null}

      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  center: { justifyContent: "center", alignItems: "center" },
  content: { padding: 16, paddingBottom: 40, gap: 14 },
  statusCard: {
    borderRadius: 18,
    padding: 16,
    backgroundColor: "rgba(0,0,0,0.03)",
  },
  statusIconRow: { flexDirection: "row", gap: 14, alignItems: "flex-start" },
  statusTitle: { fontSize: 18, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, marginBottom: 4 },
  statusSubtitle: { fontSize: 14, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular, lineHeight: 20 },
  statusActions: { marginTop: 14 },
  panel: { borderRadius: 18, padding: 16, borderWidth: 1, borderColor: "#E5E7EB", backgroundColor: "#FFFFFF" },
  panelTitle: { fontSize: 16, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, marginBottom: 6 },
  panelText: { fontSize: 14, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular, lineHeight: 20, marginBottom: 10 },
  qrWrapper: { alignItems: "center", paddingVertical: 18, borderRadius: 14, borderWidth: 1, borderColor: "#E5E7EB", backgroundColor: "#FFFFFF" },
  secretBox: { paddingVertical: 12, paddingHorizontal: 12, borderRadius: 14, borderWidth: 1, borderColor: "#E5E7EB", backgroundColor: "#F9FAFB" },
  secretValue: { textAlign: "center", fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, fontSize: 14 },
  codesBox: { borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 14, padding: 12, backgroundColor: "#F9FAFB", gap: 6 },
  codeItem: { fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, fontSize: 14, color: "#111827" },
  dangerOutline: {
    borderWidth: 2,
    borderColor: "#EF4444",
    borderRadius: 999,
    paddingVertical: 14,
    paddingHorizontal: 16,
    flexDirection: "row",
    justifyContent: "center",
    alignItems: "center",
    gap: 10,
  },
  dangerOutlineText: { color: "#EF4444", fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, fontSize: 16 },
  dangerFilled: {
    backgroundColor: "#EF4444",
    borderRadius: 999,
    paddingVertical: 14,
    paddingHorizontal: 16,
    flexDirection: "row",
    justifyContent: "center",
    alignItems: "center",
    gap: 10,
  },
  dangerFilledText: { color: "#FFFFFF", fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, fontSize: 16 },
  secondaryBtn: {
    borderRadius: 999,
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderWidth: 1,
    borderColor: "#D1D5DB",
    backgroundColor: "#F9FAFB",
    alignItems: "center",
    justifyContent: "center",
  },
  secondaryBtnText: { color: "#374151", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, fontSize: 16 },
});
