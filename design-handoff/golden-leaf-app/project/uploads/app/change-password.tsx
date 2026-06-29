import React, { useState, useCallback } from "react";
import { StyleSheet, View, Pressable, Platform, Alert, ScrollView } from "react-native";
import { useRouter } from "expo-router";
import AsyncStorage from "@react-native-async-storage/async-storage";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton, TextField } from "@/components/ui/form-controls";
import { LeafLoader } from "@/components/LeafLoader";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { extractErrorMessage } from "@/lib/inspection-storage";

const API_BASE_URL_KEY = "tbz:portalBaseUrl:v1";
const AUTH_ACCESS_KEY = "tbz:portalAccessToken:v1";

function normalizeBaseUrl(raw: string) {
  return raw.trim().replace(/\/+$/, "");
}

function sanitizePortalBaseUrl(raw: string) {
  const value = normalizeBaseUrl(raw);
  if (!value) return "";
  try {
    const url = new URL(value);
    return normalizeBaseUrl(`${url.protocol}//${url.host}`);
  } catch {
    try {
      const needsHttp =
        value.startsWith("localhost") ||
        value.startsWith("127.0.0.1") ||
        /^\d{1,3}(\.\d{1,3}){3}(:\d+)?$/.test(value);
      const url = new URL(`${needsHttp ? "http" : "https"}://${value}`);
      return normalizeBaseUrl(`${url.protocol}//${url.host}`);
    } catch {
      return value;
    }
  }
}

function runtimePortalBaseUrl(raw: string) {
  const baseUrl = sanitizePortalBaseUrl(raw);
  if (!baseUrl) return "";
  try {
    const url = new URL(baseUrl);
    const host = url.hostname;
    if (Platform.OS === "android" && (host === "localhost" || host === "127.0.0.1")) {
      url.hostname = "10.0.2.2";
      return normalizeBaseUrl(url.toString());
    }
    return normalizeBaseUrl(url.toString());
  } catch {
    return baseUrl;
  }
}

export default function ChangePasswordScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";

  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChangePassword = async () => {
    if (!oldPassword || !newPassword || !confirmPassword) {
      Alert.alert("Missing Fields", "Please complete all fields.");
      return;
    }

    if (newPassword !== confirmPassword) {
      Alert.alert("Mismatch", "New password and confirm password do not match.");
      return;
    }

    if (newPassword.length < 8) {
      Alert.alert("Weak Password", "New password must be at least 8 characters long.");
      return;
    }

    setLoading(true);
    try {
      const savedBase = (await AsyncStorage.getItem(API_BASE_URL_KEY)) ?? "";
      const baseUrl = runtimePortalBaseUrl(savedBase);
      const savedAccess = (await AsyncStorage.getItem(AUTH_ACCESS_KEY)) ?? "";

      if (!baseUrl || !savedAccess) {
        Alert.alert("Authentication Error", "You are not securely connected. Please log in again.");
        return;
      }

      const resp = await fetch(`${baseUrl}/api/v1/auth/users/change-password/`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Accept": "application/json",
          "Authorization": `Bearer ${savedAccess}`,
        },
        body: JSON.stringify({
          old_password: oldPassword,
          new_password: newPassword,
        }),
      });

      const raw = await resp.text();
      const extracted = extractErrorMessage(raw) || "";

      if (resp.ok) {
        Alert.alert("Success", "Your password has been changed successfully.", [
          { text: "OK", onPress: () => router.back() }
        ]);
      } else {
        Alert.alert("Request failed", extracted || "Request failed. Please try again.");
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Network request failed.";
      Alert.alert("Network error", extractErrorMessage(msg) || "Network error. Please try again.");
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
        <ThemedText style={[styles.headerTitle, { color: Colors[theme].surface }]}>Change Password</ThemedText>
        <View style={{ width: 48 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <View style={styles.iconBox}>
          <MaterialIcons name="lock-reset" size={64} color={Colors[theme].primary} />
          <ThemedText style={styles.title}>Secure your Account</ThemedText>
          <ThemedText style={styles.subtitle}>Enter your current password and pick a strong new password to keep your information safe.</ThemedText>
        </View>

        <View style={styles.formLayer}>
          <TextField
            label="Current Password"
            value={oldPassword}
            onChangeText={setOldPassword}
            secureTextEntry
            placeholder="Enter your old password"
          />
          <TextField
            label="New Password"
            value={newPassword}
            onChangeText={setNewPassword}
            secureTextEntry
            placeholder="Enter a new secure password"
          />
          <TextField
            label="Confirm New Password"
            value={confirmPassword}
            onChangeText={setConfirmPassword}
            secureTextEntry
            placeholder="Re-type your new password"
          />

          <View style={styles.actionBlock}>
            {loading ? (
              <View style={{ marginTop: 20, alignItems: "center" }}>
                <LeafLoader size={36} />
              </View>
            ) : (
              <PrimaryButton title="Update Password" onPress={handleChangePassword} />
            )}
          </View>
        </View>
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingTop: Platform.OS === "android" ? 40 : 60, paddingBottom: 16, paddingHorizontal: 8 },
  backButton: { padding: 8, width: 48, alignItems: "center" },
  headerTitle: { fontSize: 22, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular },
  content: { padding: 24, paddingBottom: 100 },
  iconBox: { alignItems: "center", marginBottom: 32, marginTop: 10 },
  title: { fontSize: 24, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, textAlign: "center", marginVertical: 12 },
  subtitle: { fontSize: 15, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular, textAlign: "center", color: '#6B7280', lineHeight: 22, paddingHorizontal: 10 },
  formLayer: { gap: 16 },
  actionBlock: { paddingTop: 20 }
});
