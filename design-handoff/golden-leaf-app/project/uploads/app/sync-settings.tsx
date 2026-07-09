import AsyncStorage from "@react-native-async-storage/async-storage";
import { useEffect, useState, useCallback } from "react";
import {
  ActivityIndicator,
  Alert,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Switch,
  View,
} from "react-native";
import { useFocusEffect, useRouter } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { Colors } from "@/constants/theme";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { API_BASE_URL_KEY, runtimePortalBaseUrl } from "@/lib/inspection-storage";
import {
  getLastOfflineSyncAt,
  getPendingOfflineWorkCount,
  runOfflineSyncIfNeeded,
} from "@/lib/offline-sync";
import {
  getSyncPreferences,
  setSyncPreferences,
  type SyncPreferences,
} from "@/lib/offline-preferences";

function formatLastSync(iso: string): string {
  if (!iso) return "Never";
  try {
    const ms = Date.now() - new Date(iso).getTime();
    if (ms < 60_000) return "Just now";
    if (ms < 3_600_000) return `${Math.floor(ms / 60_000)} min ago`;
    if (ms < 86_400_000) return `${Math.floor(ms / 3_600_000)} h ago`;
    return new Date(iso).toLocaleDateString();
  } catch {
    return iso;
  }
}

export default function SyncSettingsScreen() {
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const router = useRouter();

  const [prefs, setPrefs] = useState<SyncPreferences | null>(null);
  const [pendingCount, setPendingCount] = useState(0);
  const [lastSyncedAt, setLastSyncedAt] = useState("");
  const [syncing, setSyncing] = useState(false);
  const [statusMsg, setStatusMsg] = useState("");
  const [portalBaseUrl, setPortalBaseUrl] = useState("");

  useEffect(() => {
    void (async () => {
      setPrefs(await getSyncPreferences());
      setPendingCount(await getPendingOfflineWorkCount());
      setLastSyncedAt(await getLastOfflineSyncAt());
    })();
  }, []);

  useFocusEffect(
    useCallback(() => {
      let cancelled = false;
      void (async () => {
        const saved = (await AsyncStorage.getItem(API_BASE_URL_KEY)) ?? "";
        const resolved = runtimePortalBaseUrl(saved);
        if (!cancelled) setPortalBaseUrl(resolved);
      })();
      return () => {
        cancelled = true;
      };
    }, []),
  );

  const refreshStatus = async () => {
    setPendingCount(await getPendingOfflineWorkCount());
    setLastSyncedAt(await getLastOfflineSyncAt());
  };

  const togglePref = async (key: keyof SyncPreferences, value: boolean) => {
    const next = await setSyncPreferences({ [key]: value });
    setPrefs(next);
  };

  const onSyncNow = async () => {
    if (syncing) return;
    setSyncing(true);
    setStatusMsg("Starting…");
    try {
      const result = await runOfflineSyncIfNeeded(setStatusMsg, { manual: true });
      switch (result.reason) {
        case "ok":
        case "up_to_date":
          setStatusMsg("Sync complete.");
          break;
        case "no_session":
          Alert.alert("Login required", "Please log in to the portal before syncing.");
          break;
        case "offline":
          Alert.alert("No connection", "Connect to the internet and try again.");
          break;
        case "wifi_only":
          Alert.alert("Wi-Fi only", "Sync is restricted to Wi-Fi. Connect to Wi-Fi or change the setting.");
          break;
        case "auto_disabled":
          setStatusMsg("Sync ran (auto sync is paused).");
          break;
        case "upload_only_metered":
          setStatusMsg("Uploads complete (cellular detected — downloads skipped).");
          break;
        default:
          setStatusMsg("");
      }
    } finally {
      await refreshStatus();
      setTimeout(() => setSyncing(false), 600);
    }
  };

  if (!prefs) {
    return (
      <ThemedView style={styles.container}>
        <ActivityIndicator />
      </ThemedView>
    );
  }

  return (
    <ThemedView style={styles.container}>
      <View style={[styles.header, { backgroundColor: Colors[theme].primary }]}>
        <Pressable onPress={() => router.back()} style={styles.backButton}>
          <MaterialIcons name="chevron-left" size={32} color={Colors[theme].surface} />
        </Pressable>
        <ThemedText style={[styles.headerTitle, { color: Colors[theme].surface }]}>
          Sync Settings
        </ThemedText>
        <View style={{ width: 48 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <View style={[styles.card, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          <ThemedText style={styles.sectionLabel}>Status</ThemedText>
          <View style={styles.statusRow}>
            <ThemedText style={styles.label} numberOfLines={1} ellipsizeMode="tail">
              Pending records
            </ThemedText>
            <ThemedText style={styles.value} numberOfLines={1} ellipsizeMode="tail">
              {pendingCount}
            </ThemedText>
          </View>
          <View style={styles.statusRow}>
            <ThemedText style={styles.label} numberOfLines={1} ellipsizeMode="tail">
              Last sync
            </ThemedText>
            <ThemedText style={styles.value} numberOfLines={1} ellipsizeMode="tail">
              {formatLastSync(lastSyncedAt)}
            </ThemedText>
          </View>
        </View>

        <Pressable
          onPress={onSyncNow}
          disabled={syncing}
          style={({ pressed }) => [
            styles.syncButton,
            { backgroundColor: Colors[theme].primary, opacity: syncing || pressed ? 0.7 : 1 },
          ]}
        >
          {syncing ? (
            <ActivityIndicator color={Colors[theme].surface} />
          ) : (
            <MaterialIcons name="sync" size={20} color={Colors[theme].surface} />
          )}
          <ThemedText style={[styles.syncButtonText, { color: Colors[theme].surface }]} numberOfLines={1} ellipsizeMode="tail">
            {syncing ? statusMsg || "Syncing…" : "Sync now"}
          </ThemedText>
        </Pressable>

        <View style={[styles.card, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          <ThemedText style={styles.sectionLabel}>Preferences</ThemedText>

          <View style={styles.toggleRow}>
            <View style={{ flex: 1 }}>
              <ThemedText style={styles.toggleTitle}>Automatic sync</ThemedText>
              <ThemedText style={[styles.toggleHint, { color: Colors[theme].muted }]}>
                Sync runs in the background while you work. Turn off to sync only when you tap Sync now.
              </ThemedText>
            </View>
            <Switch
              value={prefs.autoSyncEnabled}
              onValueChange={(v) => togglePref("autoSyncEnabled", v)}
              trackColor={{ true: Colors[theme].primary, false: "#9CA3AF" }}
            />
          </View>

          <View style={styles.divider} />

          <View style={styles.toggleRow}>
            <View style={{ flex: 1 }}>
              <ThemedText style={styles.toggleTitle}>Sync only on Wi-Fi</ThemedText>
              <ThemedText style={[styles.toggleHint, { color: Colors[theme].muted }]}>
                Skip syncing on cellular data. Useful when roaming on metered plans.
              </ThemedText>
            </View>
            <Switch
              value={prefs.syncOnlyOnWifi}
              onValueChange={(v) => togglePref("syncOnlyOnWifi", v)}
              trackColor={{ true: Colors[theme].primary, false: "#9CA3AF" }}
            />
          </View>
        </View>

        <View style={[styles.card, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          <ThemedText style={styles.sectionLabel}>Portal</ThemedText>
          <View style={styles.statusRow}>
            <ThemedText style={styles.label} numberOfLines={1} ellipsizeMode="tail">
              Base URL
            </ThemedText>
            <ThemedText style={styles.value} numberOfLines={1} ellipsizeMode="tail">
              {portalBaseUrl || "Not set"}
            </ThemedText>
          </View>
          <Pressable
            onPress={() => router.push("/portal-login" as any)}
            style={({ pressed }) => [
              styles.portalButton,
              { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface },
              pressed && { opacity: 0.7 },
            ]}
          >
            <MaterialIcons name="settings" size={18} color={Colors[theme].primary} />
            <ThemedText style={[styles.portalButtonText, { color: Colors[theme].primary }]} numberOfLines={1} ellipsizeMode="tail">
              Portal settings
            </ThemedText>
          </Pressable>
        </View>

        <ThemedText style={[styles.footer, { color: Colors[theme].muted }]}>
          Pending records are kept until they sync successfully or you discard them
          from the queue screens.
        </ThemedText>
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 16,
    paddingTop: Platform.OS === "android" ? 40 : 56,
    paddingBottom: 16,
  },
  headerTitle: { fontSize: 20, fontWeight: "600" },
  backButton: { padding: 8 },
  content: { padding: 16, gap: 16 },
  card: {
    borderRadius: 12,
    borderWidth: 1,
    padding: 16,
    overflow: "hidden",
  },
  sectionLabel: { fontSize: 12, opacity: 0.7, marginBottom: 12, letterSpacing: 0.5, textTransform: "uppercase" },
  statusRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    paddingVertical: 8,
  },
  label: { fontSize: 14, flexShrink: 1, paddingRight: 12 },
  value: { fontSize: 14, fontWeight: "600", flexShrink: 0 },
  syncButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 10,
    paddingVertical: 14,
    borderRadius: 12,
  },
  syncButtonText: { fontSize: 15, fontWeight: "600" },
  portalButton: {
    marginTop: 12,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 10,
    paddingVertical: 12,
    borderRadius: 12,
    borderWidth: 1,
  },
  portalButtonText: { fontSize: 14, fontWeight: "600" },
  toggleRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 8,
    gap: 16,
  },
  toggleTitle: { fontSize: 15, fontWeight: "500", marginBottom: 4 },
  toggleHint: { fontSize: 12, lineHeight: 16 },
  divider: { height: StyleSheet.hairlineWidth, backgroundColor: "#E5E7EB", marginVertical: 8 },
  footer: { fontSize: 12, marginTop: 8, lineHeight: 16, paddingHorizontal: 4 },
});
